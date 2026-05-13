package com.cashi.osiptelvalidation.application.services;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerRepository;
import com.cashi.osiptelvalidation.application.internal.outboundservices.OsiptelWorkerClient;
import com.cashi.osiptelvalidation.domain.model.aggregates.OsiptelValidation;
import com.cashi.osiptelvalidation.domain.model.commands.RecordValidationResultCommand;
import com.cashi.osiptelvalidation.domain.services.OsiptelValidationCommandService;
import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import com.cashi.osiptelvalidation.infrastructure.persistence.jpa.repositories.OsiptelValidationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Dispatcher que mueve filas PENDING → IN_PROGRESS y las envía al worker.
 *
 * Flujo por ciclo:
 *  1. claimBatch() (transacción corta): SELECT FOR UPDATE SKIP LOCKED + IN_PROGRESS.
 *  2. Por cada fila claimed: dispatchOne() async: lookup DNI plaintext desde
 *     clientes.documento (via source_customer_id), llamar al worker,
 *     persistir resultado.
 *
 * Privacidad: el DNI plaintext sólo existe en memoria durante la llamada al
 * worker; nunca se persiste en osiptel_validation_log.
 *
 * Si una validation no tiene source_customer_id (encolado manual sin link),
 * la rechaza con FAILED y motivo "no-customer-link".
 */
@Service
public class OsiptelDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(OsiptelDispatcherService.class);

    private final OsiptelValidationRepository validationRepository;
    private final OsiptelValidationCommandService commandService;
    private final OsiptelWorkerClient workerClient;
    private final CustomerRepository customerRepository;
    private final OsiptelAuditService audit;
    private final OsiptelProperties properties;
    private final String workerId;

    public OsiptelDispatcherService(OsiptelValidationRepository validationRepository,
                                    OsiptelValidationCommandService commandService,
                                    OsiptelWorkerClient workerClient,
                                    CustomerRepository customerRepository,
                                    OsiptelAuditService audit,
                                    OsiptelProperties properties) {
        this.validationRepository = validationRepository;
        this.commandService = commandService;
        this.workerClient = workerClient;
        this.customerRepository = customerRepository;
        this.audit = audit;
        this.properties = properties;
        this.workerId = "dispatcher-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Scheduled(fixedDelayString = "${cashi.osiptel.dispatcher-interval-ms:30000}")
    public void dispatchCycle() {
        if (!properties.isDispatcherEnabled() || !properties.isLegalReviewSignedOff()) {
            return;
        }

        int reclaimed = commandService.reclaimStuckInProgress(properties.getStuckThresholdMinutes());
        if (reclaimed > 0) {
            audit.recordReclaim(reclaimed);
        }

        List<OsiptelValidation> claimed = claimBatch();
        if (claimed.isEmpty()) {
            return;
        }

        for (OsiptelValidation v : claimed) {
            dispatchOneAsync(v.getId());
        }
    }

    @Transactional
    protected List<OsiptelValidation> claimBatch() {
        List<OsiptelValidation> rows = validationRepository.claimPendingForUpdate(
                properties.getDispatcherBatchSize());
        for (OsiptelValidation v : rows) {
            v.markInProgress(workerId);
        }
        if (!rows.isEmpty()) {
            validationRepository.saveAll(rows);
            log.debug("Osiptel dispatcher claim: {} filas marcadas IN_PROGRESS", rows.size());
        }
        return rows;
    }

    @Async("osiptelExecutor")
    public void dispatchOneAsync(Long validationId) {
        OsiptelValidation v = validationRepository.findById(validationId).orElse(null);
        if (v == null) {
            log.warn("Osiptel dispatch: validation {} desapareció", validationId);
            return;
        }

        // Resolver DNI plaintext desde clientes.documento
        if (v.getSourceCustomerId() == null) {
            commandService.recordResult(noCustomerLinkResult(v));
            return;
        }
        Customer customer = customerRepository.findById(v.getSourceCustomerId()).orElse(null);
        if (customer == null || customer.getDocument() == null || customer.getDocument().isBlank()) {
            commandService.recordResult(noCustomerLinkResult(v));
            return;
        }

        String requestId = "v" + v.getId() + "-a" + v.getAttempts();
        OsiptelWorkerClient.WorkerCheckResult result =
                workerClient.check(requestId, customer.getDocument(), v.getDniType());

        commandService.recordResult(new RecordValidationResultCommand(
                v.getId(),
                workerId,
                result.status(),
                result.lines(),
                (int) result.latencyMs(),
                result.captchaAttempts(),
                result.httpStatus(),
                result.errorDetail()
        ));

        audit.recordValidation(result.status(), null, result.latencyMs());
        if (result.captchaAttempts() != null && result.captchaAttempts() > 0) {
            audit.recordCaptchaOutcome("OK".equals(result.status()) || "NOT_FOUND".equals(result.status())
                    ? "solved" : "failed");
        }
    }

    private RecordValidationResultCommand noCustomerLinkResult(OsiptelValidation v) {
        return new RecordValidationResultCommand(
                v.getId(), workerId, "ERROR", null, 0, 0, 0, "no-customer-link");
    }
}
