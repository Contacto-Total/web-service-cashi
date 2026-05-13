package com.cashi.osiptelvalidation.application.services;

import com.cashi.osiptelvalidation.application.internal.outboundservices.OsiptelWorkerClient;
import com.cashi.osiptelvalidation.domain.model.aggregates.OsiptelValidation;
import com.cashi.osiptelvalidation.domain.model.commands.RecordValidationResultCommand;
import com.cashi.osiptelvalidation.domain.model.valueobjects.OperatorCode;
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
 *  1. claimBatch() (transacción corta): SELECT FOR UPDATE SKIP LOCKED, marcar IN_PROGRESS, commit.
 *  2. Por cada fila claimed: dispatchOne() (transacción propia): llamar al worker, persistir resultado.
 *
 * El @Scheduled no entra si la app no es la "owner" del cron (feature flag) o
 * si Legal no ha firmado. Ambos defaults son false → el dispatcher arranca apagado.
 *
 * Multi-instancia: SKIP LOCKED + UNIQUE(phone, status) protegen contra trabajo duplicado.
 */
@Service
public class OsiptelDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(OsiptelDispatcherService.class);

    private final OsiptelValidationRepository validationRepository;
    private final OsiptelValidationCommandService commandService;
    private final OsiptelWorkerClient workerClient;
    private final OsiptelAuditService audit;
    private final OsiptelProperties properties;
    private final String workerId;

    public OsiptelDispatcherService(OsiptelValidationRepository validationRepository,
                                    OsiptelValidationCommandService commandService,
                                    OsiptelWorkerClient workerClient,
                                    OsiptelAuditService audit,
                                    OsiptelProperties properties) {
        this.validationRepository = validationRepository;
        this.commandService = commandService;
        this.workerClient = workerClient;
        this.audit = audit;
        this.properties = properties;
        this.workerId = "dispatcher-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Ciclo principal. Default 30s.
     * Si el flag está apagado, no hace nada (no genera carga en BD ni en red).
     */
    @Scheduled(fixedDelayString = "${cashi.osiptel.dispatcher-interval-ms:30000}")
    public void dispatchCycle() {
        if (!properties.isDispatcherEnabled() || !properties.isLegalReviewSignedOff()) {
            return;
        }

        // 1. Reclamo de filas huérfanas IN_PROGRESS
        int reclaimed = commandService.reclaimStuckInProgress(properties.getStuckThresholdMinutes());
        if (reclaimed > 0) {
            audit.recordReclaim(reclaimed);
        }

        // 2. Claim de lote PENDING
        List<OsiptelValidation> claimed = claimBatch();
        if (claimed.isEmpty()) {
            return;
        }

        // 3. Dispatch async por fila (cada uno con su propia transacción + llamada HTTP)
        for (OsiptelValidation v : claimed) {
            dispatchOneAsync(v.getId());
        }
    }

    /**
     * Reclama un lote en una transacción corta para liberar locks antes de las llamadas HTTP largas.
     */
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

    /**
     * Llama al worker y registra resultado.
     * @Async para que el @Scheduled siga reclamando lotes sin esperar al worker.
     */
    @Async("osiptelExecutor")
    public void dispatchOneAsync(Long validationId) {
        OsiptelValidation v = validationRepository.findById(validationId).orElse(null);
        if (v == null) {
            log.warn("Osiptel dispatch: validation {} desapareció", validationId);
            return;
        }

        // El DNI plaintext no está en BD - el worker recibe solo el phone.
        // En este flujo standalone (Fase 1), si no se conoce el DNI no se puede calcular dni_match;
        // el worker igual valida operador y existencia. dni_match queda NULL.
        // En Fase 3, al integrar con clientes, se pasará el DNI desde el caller.
        String dni = null;  // En el path de selector nocturno el DNI viaja en memoria; aquí se rehidratará en Fase 3.

        String requestId = "v" + v.getId() + "-a" + v.getAttempts();
        OsiptelWorkerClient.WorkerCheckResult result =
                workerClient.check(requestId, v.getPhone(), dni);

        OperatorCode operatorCode = result.operator() == null ? null
                : OperatorCode.fromPortalText(result.operator());

        commandService.recordResult(new RecordValidationResultCommand(
                v.getId(),
                workerId,
                result.status(),
                operatorCode,
                result.dniMatch(),
                (int) result.latencyMs(),
                result.captchaAttempts(),
                result.httpStatus(),
                result.errorDetail()
        ));

        audit.recordValidation(result.status(),
                operatorCode == null ? null : operatorCode.name(),
                result.latencyMs());
        if (result.captchaAttempts() != null && result.captchaAttempts() > 0) {
            audit.recordCaptchaOutcome("OK".equals(result.status()) || "NOT_FOUND".equals(result.status())
                    ? "solved" : "failed");
        }
    }
}
