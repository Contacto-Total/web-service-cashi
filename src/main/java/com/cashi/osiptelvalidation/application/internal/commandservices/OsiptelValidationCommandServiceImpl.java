package com.cashi.osiptelvalidation.application.internal.commandservices;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import com.cashi.customermanagement.domain.model.entities.ContactMethod;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ContactMethodRepository;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerRepository;
import com.cashi.osiptelvalidation.domain.model.aggregates.OsiptelValidation;
import com.cashi.osiptelvalidation.domain.model.commands.EnqueueOsiptelBatchCommand;
import com.cashi.osiptelvalidation.domain.model.commands.RecordValidationResultCommand;
import com.cashi.osiptelvalidation.domain.model.entities.OsiptelPhoneMatch;
import com.cashi.osiptelvalidation.domain.model.entities.OsiptelValidationAttempt;
import com.cashi.osiptelvalidation.domain.model.valueobjects.CooldownPolicy;
import com.cashi.osiptelvalidation.domain.model.valueobjects.OperatorCode;
import com.cashi.osiptelvalidation.domain.model.valueobjects.OsiptelLine;
import com.cashi.osiptelvalidation.domain.model.valueobjects.PhoneNumber;
import com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus;
import com.cashi.osiptelvalidation.domain.services.OsiptelValidationCommandService;
import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import com.cashi.osiptelvalidation.infrastructure.persistence.jpa.repositories.OsiptelPhoneMatchRepository;
import com.cashi.osiptelvalidation.infrastructure.persistence.jpa.repositories.OsiptelValidationAttemptRepository;
import com.cashi.osiptelvalidation.infrastructure.persistence.jpa.repositories.OsiptelValidationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OsiptelValidationCommandServiceImpl implements OsiptelValidationCommandService {

    private static final Logger log = LoggerFactory.getLogger(OsiptelValidationCommandServiceImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final OsiptelValidationRepository validationRepository;
    private final OsiptelValidationAttemptRepository attemptRepository;
    private final OsiptelPhoneMatchRepository phoneMatchRepository;
    private final ContactMethodRepository contactMethodRepository;
    private final CustomerRepository customerRepository;
    private final DniHashService dniHashService;
    private final OsiptelProperties properties;

    public OsiptelValidationCommandServiceImpl(OsiptelValidationRepository validationRepository,
                                               OsiptelValidationAttemptRepository attemptRepository,
                                               OsiptelPhoneMatchRepository phoneMatchRepository,
                                               ContactMethodRepository contactMethodRepository,
                                               CustomerRepository customerRepository,
                                               DniHashService dniHashService,
                                               OsiptelProperties properties) {
        this.validationRepository = validationRepository;
        this.attemptRepository = attemptRepository;
        this.phoneMatchRepository = phoneMatchRepository;
        this.contactMethodRepository = contactMethodRepository;
        this.customerRepository = customerRepository;
        this.dniHashService = dniHashService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public EnqueueResult enqueueBatch(EnqueueOsiptelBatchCommand command) {
        String batchId = UUID.randomUUID().toString();
        int enqueued = 0;
        int skipped = 0;

        for (EnqueueOsiptelBatchCommand.DocumentEntry entry : command.entries()) {
            try {
                if (entry.dni() == null || entry.dni().isBlank()) {
                    skipped++;
                    continue;
                }
                String dniHash = dniHashService.hash(entry.dni());

                if (validationRepository.existsByDniHashAndStatusIn(
                        dniHash,
                        List.of(ValidationStatus.PENDING, ValidationStatus.IN_PROGRESS))) {
                    skipped++;
                    continue;
                }

                OsiptelValidation validation = new OsiptelValidation(
                        dniHash,
                        entry.dniType(),
                        entry.customerId(),
                        entry.subPortfolioId()
                );
                validationRepository.save(validation);
                enqueued++;
            } catch (DataIntegrityViolationException e) {
                // Carrera contra UNIQUE KEY: otro proceso encoló al mismo tiempo
                skipped++;
            } catch (Exception e) {
                log.warn("Skip entry inválido en batch {}: {}", batchId, e.getMessage());
                skipped++;
            }
        }

        log.info("Osiptel batch {} encolado: {} aceptados, {} rechazados", batchId, enqueued, skipped);
        return new EnqueueResult(enqueued, skipped, batchId);
    }

    @Override
    @Transactional
    public void recordResult(RecordValidationResultCommand cmd) {
        OsiptelValidation validation = validationRepository.findById(cmd.validationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "OsiptelValidation no encontrada: " + cmd.validationId()));

        if (validation.getStatus() != ValidationStatus.IN_PROGRESS) {
            log.warn("recordResult sobre validación {} en estado {} (esperaba IN_PROGRESS). Ignorando.",
                    cmd.validationId(), validation.getStatus());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        CooldownPolicy policy = new CooldownPolicy(
                Duration.ofDays(properties.getCooldownOkDays()),
                Duration.ofDays(properties.getCooldownNotFoundDays()),
                Duration.ofDays(properties.getCooldownFailedDays())
        );

        switch (cmd.resultStatus()) {
            case "OK" -> handleOk(validation, cmd.lines(), policy.nextCooldownFor(ValidationStatus.OK, now));
            case "NOT_FOUND" -> validation.recordNotFound(policy.nextCooldownFor(ValidationStatus.NOT_FOUND, now));
            case "CAPTCHA_FAIL", "BANNED", "ERROR" -> {
                LocalDateTime cooldown = policy.nextCooldownFor(ValidationStatus.FAILED, now);
                validation.recordFailed(cmd.resultStatus() + ":" + nullable(cmd.errorDetail()), cooldown);
                if (validation.getAttempts() >= properties.getMaxAttempts()) {
                    validation.markExpired("max_attempts_exceeded");
                }
            }
            default -> {
                log.warn("resultStatus desconocido: {}. Marcando FAILED.", cmd.resultStatus());
                validation.recordFailed("UNKNOWN:" + cmd.resultStatus(),
                        policy.nextCooldownFor(ValidationStatus.FAILED, now));
            }
        }
        validationRepository.save(validation);

        // Persistir intento (best-effort, no rompe el flujo si falla)
        try {
            attemptRepository.save(new OsiptelValidationAttempt(
                    validation.getId(),
                    validation.getAttempts(),
                    cmd.httpStatus() == null ? null : cmd.httpStatus().shortValue(),
                    cmd.captchaAttempts() == null ? 0 : cmd.captchaAttempts().shortValue(),
                    cmd.latencyMs(),
                    cmd.resultStatus(),
                    cmd.errorDetail(),
                    cmd.workerId()
            ));
        } catch (Exception e) {
            log.warn("No se pudo persistir attempt para validation {}: {}",
                    validation.getId(), e.getMessage());
        }
    }

    /**
     * Resultado OK: persiste lines_json en el aggregate, y cruza con
     * metodos_contacto del cliente (si sourceCustomerId está presente)
     * para crear las filas en osiptel_phone_match.
     */
    private void handleOk(OsiptelValidation validation, List<OsiptelLine> lines, LocalDateTime cooldownUntil) {
        if (lines == null) lines = List.of();
        String linesJson;
        try {
            linesJson = JSON.writeValueAsString(lines);
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar lines_json, persistiendo lista vacía: {}", e.getMessage());
            linesJson = "[]";
        }
        validation.recordOk(linesJson, lines.size(), cooldownUntil);

        if (validation.getSourceCustomerId() == null || lines.isEmpty()) {
            return;
        }

        // Cruzar contra teléfonos del cliente
        Map<String, OsiptelLine> prefixIndex = new HashMap<>();
        for (OsiptelLine line : lines) {
            // Si el prefijo es < 5 dígitos, paddear o saltar; aquí asumimos 5
            String p = line.phonePrefix();
            if (p.length() >= 5) prefixIndex.put(p.substring(0, 5), line);
        }

        List<ContactMethod> phones = contactMethodRepository.findByCustomerIdAndContactType(
                validation.getSourceCustomerId(), "telefono");

        for (ContactMethod cm : phones) {
            String value = cm.getValue();
            if (!PhoneNumber.isValid(value)) continue;
            PhoneNumber pn = PhoneNumber.of(value);
            String prefix = pn.value().substring(0, 5);
            OsiptelLine matched = prefixIndex.get(prefix);

            OsiptelPhoneMatch match = new OsiptelPhoneMatch(
                    validation.getId(),
                    pn.value(),
                    prefix,
                    matched != null,
                    matched == null ? null : matched.operator(),
                    matched == null ? null : matched.modality()
            );
            phoneMatchRepository.save(match);
        }
    }

    @Override
    @Transactional
    public List<ClaimedJob> claimJobs(String workerId, int limit) {
        if (limit <= 0) return List.of();
        if (limit > 20) limit = 20;

        List<OsiptelValidation> claimed = validationRepository.claimPendingForUpdate(limit);
        List<ClaimedJob> jobs = new ArrayList<>(claimed.size());
        List<OsiptelValidation> toSave = new ArrayList<>(claimed.size());

        for (OsiptelValidation v : claimed) {
            // Resolver DNI plaintext desde clientes.documento
            if (v.getSourceCustomerId() == null) {
                v.markInProgress(workerId);
                toSave.add(v);
                // Sin customer link no podemos enviar DNI: fallar inmediato
                recordResult(new RecordValidationResultCommand(
                        v.getId(), workerId, "ERROR", null, 0, 0, 0, "no-customer-link"));
                continue;
            }
            Customer customer = customerRepository.findById(v.getSourceCustomerId()).orElse(null);
            if (customer == null || customer.getDocument() == null || customer.getDocument().isBlank()) {
                v.markInProgress(workerId);
                toSave.add(v);
                recordResult(new RecordValidationResultCommand(
                        v.getId(), workerId, "ERROR", null, 0, 0, 0, "no-customer-link"));
                continue;
            }
            v.markInProgress(workerId);
            toSave.add(v);
            jobs.add(new ClaimedJob(v.getId(), customer.getDocument(), v.getDniType()));
        }

        if (!toSave.isEmpty()) {
            validationRepository.saveAll(toSave);
        }
        if (!jobs.isEmpty()) {
            log.info("Osiptel claim por workerId={}: {} jobs servidos", workerId, jobs.size());
        }
        return jobs;
    }

    @Override
    @Transactional
    public int reclaimStuckInProgress(int olderThanMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(olderThanMinutes);
        List<OsiptelValidation> stuck = validationRepository.findStuckInProgress(cutoff);
        for (OsiptelValidation v : stuck) {
            v.reclaimStuck();
        }
        if (!stuck.isEmpty()) {
            validationRepository.saveAll(stuck);
            log.info("Osiptel reclaim: {} validaciones devueltas a PENDING", stuck.size());
        }
        return stuck.size();
    }

    private static String nullable(String s) {
        return s == null ? "" : s;
    }
}
