package com.cashi.osiptelvalidation.application.internal.commandservices;

import com.cashi.osiptelvalidation.domain.model.aggregates.OsiptelValidation;
import com.cashi.osiptelvalidation.domain.model.commands.EnqueueOsiptelBatchCommand;
import com.cashi.osiptelvalidation.domain.model.commands.RecordValidationResultCommand;
import com.cashi.osiptelvalidation.domain.model.entities.OsiptelValidationAttempt;
import com.cashi.osiptelvalidation.domain.model.valueobjects.CooldownPolicy;
import com.cashi.osiptelvalidation.domain.model.valueobjects.PhoneNumber;
import com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus;
import com.cashi.osiptelvalidation.domain.services.OsiptelValidationCommandService;
import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import com.cashi.osiptelvalidation.infrastructure.persistence.jpa.repositories.OsiptelValidationAttemptRepository;
import com.cashi.osiptelvalidation.infrastructure.persistence.jpa.repositories.OsiptelValidationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OsiptelValidationCommandServiceImpl implements OsiptelValidationCommandService {

    private static final Logger log = LoggerFactory.getLogger(OsiptelValidationCommandServiceImpl.class);

    private final OsiptelValidationRepository validationRepository;
    private final OsiptelValidationAttemptRepository attemptRepository;
    private final DniHashService dniHashService;
    private final OsiptelProperties properties;

    public OsiptelValidationCommandServiceImpl(OsiptelValidationRepository validationRepository,
                                               OsiptelValidationAttemptRepository attemptRepository,
                                               DniHashService dniHashService,
                                               OsiptelProperties properties) {
        this.validationRepository = validationRepository;
        this.attemptRepository = attemptRepository;
        this.dniHashService = dniHashService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public EnqueueResult enqueueBatch(EnqueueOsiptelBatchCommand command) {
        String batchId = UUID.randomUUID().toString();
        int enqueued = 0;
        int skipped = 0;

        for (EnqueueOsiptelBatchCommand.PhoneEntry entry : command.entries()) {
            try {
                PhoneNumber phone = PhoneNumber.of(entry.phone());

                // Idempotencia: si ya hay PENDING o IN_PROGRESS, saltar.
                if (validationRepository.existsByPhoneAndStatusIn(
                        phone.value(),
                        List.of(ValidationStatus.PENDING, ValidationStatus.IN_PROGRESS))) {
                    skipped++;
                    continue;
                }

                String dniHash = dniHashService.hash(entry.dni(), entry.tenantId());
                OsiptelValidation validation = new OsiptelValidation(
                        phone.value(),
                        dniHash,
                        entry.subPortfolioId(),
                        entry.contactMethodId()
                );
                validationRepository.save(validation);
                enqueued++;
            } catch (IllegalArgumentException e) {
                log.warn("Skip phone inválido en batch {}: {}", batchId, e.getMessage());
                skipped++;
            } catch (DataIntegrityViolationException e) {
                // Carrera contra UNIQUE KEY uk_osiptel_phone_status: otro proceso encoló al mismo tiempo.
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

        // El worker devuelve resultStatus en su propio vocabulario.
        // Lo traducimos a transición del aggregate.
        switch (cmd.resultStatus()) {
            case "OK" -> validation.recordOk(
                    cmd.operator(),
                    cmd.dniMatch(),
                    policy.nextCooldownFor(ValidationStatus.OK, now));
            case "NOT_FOUND" -> validation.recordNotFound(
                    policy.nextCooldownFor(ValidationStatus.NOT_FOUND, now));
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

        // Persistir intento detallado (best-effort, fuera de la transición)
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
