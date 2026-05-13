package com.cashi.osiptelvalidation.domain.model.aggregates;

import com.cashi.osiptelvalidation.domain.model.valueobjects.OperatorCode;
import com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus;
import com.cashi.shared.domain.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Aggregate Root de la validación Osiptel para un número.
 *
 * Encapsula la máquina de estados (PENDING -> IN_PROGRESS -> OK|NOT_FOUND|FAILED|EXPIRED)
 * y las transiciones permitidas. Las mutaciones se hacen a través de los métodos de dominio,
 * no por setters libres.
 *
 * Privacidad (Ley 29733):
 *  - dni_hash: SHA-256(dni + tenant_salt). NUNCA DNI plaintext.
 *  - dni_match: boolean tri-estado (1=titular, 0=no titular, NULL=indeterminado).
 *  - NO se persiste nombre del titular bajo ninguna circunstancia.
 */
@Entity
@Table(name = "osiptel_validation_log")
@Getter
@NoArgsConstructor
public class OsiptelValidation extends AggregateRoot {

    @Column(name = "phone", length = 15, nullable = false)
    private String phone;

    @Column(name = "dni_hash", length = 64)
    private String dniHash;

    @Column(name = "dni_match")
    private Boolean dniMatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", length = 20)
    private OperatorCode operator;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ValidationStatus status;

    @Column(name = "attempts", nullable = false)
    private Short attempts;

    @Column(name = "last_error", length = 120)
    private String lastError;

    @Column(name = "enqueued_at", nullable = false)
    private LocalDateTime enqueuedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "cooldown_until")
    private LocalDateTime cooldownUntil;

    @Column(name = "source_subportfolio_id")
    private Long sourceSubPortfolioId;

    @Column(name = "source_contact_method_id")
    private Long sourceContactMethodId;

    @Column(name = "worker_id", length = 40)
    private String workerId;

    /**
     * Constructor de encolado. Estado inicial: PENDING.
     */
    public OsiptelValidation(String phone, String dniHash,
                             Long sourceSubPortfolioId, Long sourceContactMethodId) {
        super();
        this.phone = phone;
        this.dniHash = dniHash;
        this.sourceSubPortfolioId = sourceSubPortfolioId;
        this.sourceContactMethodId = sourceContactMethodId;
        this.status = ValidationStatus.PENDING;
        this.attempts = 0;
        this.enqueuedAt = LocalDateTime.now();
    }

    // ============================
    // Transiciones de estado
    // ============================

    /** PENDING -> IN_PROGRESS. Reclamo por el dispatcher antes de llamar al worker. */
    public void markInProgress(String workerId) {
        requireStatus(ValidationStatus.PENDING);
        this.status = ValidationStatus.IN_PROGRESS;
        this.workerId = workerId;
        this.startedAt = LocalDateTime.now();
        this.attempts = (short) (this.attempts + 1);
        updateTimestamp();
    }

    /** IN_PROGRESS -> OK. Resultado positivo del worker (dniMatch puede ser true o false). */
    public void recordOk(OperatorCode operator, Boolean dniMatch, LocalDateTime cooldownUntil) {
        requireStatus(ValidationStatus.IN_PROGRESS);
        this.status = ValidationStatus.OK;
        this.operator = operator;
        this.dniMatch = dniMatch;
        this.finishedAt = LocalDateTime.now();
        this.cooldownUntil = cooldownUntil;
        this.lastError = null;
        updateTimestamp();
    }

    /** IN_PROGRESS -> NOT_FOUND. El portal no encontró el número. */
    public void recordNotFound(LocalDateTime cooldownUntil) {
        requireStatus(ValidationStatus.IN_PROGRESS);
        this.status = ValidationStatus.NOT_FOUND;
        this.finishedAt = LocalDateTime.now();
        this.cooldownUntil = cooldownUntil;
        updateTimestamp();
    }

    /** IN_PROGRESS -> FAILED. Falla técnica reintentable. */
    public void recordFailed(String errorCode, LocalDateTime cooldownUntil) {
        requireStatus(ValidationStatus.IN_PROGRESS);
        this.status = ValidationStatus.FAILED;
        this.finishedAt = LocalDateTime.now();
        this.cooldownUntil = cooldownUntil;
        this.lastError = truncate(errorCode);
        updateTimestamp();
    }

    /** FAILED -> PENDING. Permite re-encolar tras cooldown. */
    public void requeueAfterFailure() {
        requireStatus(ValidationStatus.FAILED);
        this.status = ValidationStatus.PENDING;
        this.startedAt = null;
        this.finishedAt = null;
        this.workerId = null;
        updateTimestamp();
    }

    /** IN_PROGRESS huérfano (worker murió). Vuelve a PENDING. */
    public void reclaimStuck() {
        requireStatus(ValidationStatus.IN_PROGRESS);
        this.status = ValidationStatus.PENDING;
        this.startedAt = null;
        this.workerId = null;
        updateTimestamp();
    }

    /** Estado terminal por fatiga. No se reintenta. */
    public void markExpired(String reason) {
        if (this.status == ValidationStatus.EXPIRED) {
            return;
        }
        this.status = ValidationStatus.EXPIRED;
        this.finishedAt = LocalDateTime.now();
        this.lastError = truncate(reason);
        updateTimestamp();
    }

    // ============================
    // Helpers
    // ============================

    public boolean isInCooldown(LocalDateTime now) {
        return cooldownUntil != null && cooldownUntil.isAfter(now);
    }

    public boolean canBeRetried(int maxAttempts) {
        return status != ValidationStatus.EXPIRED && attempts < maxAttempts;
    }

    private void requireStatus(ValidationStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Transición inválida: estado actual=" + this.status + ", requerido=" + expected);
        }
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 120 ? s.substring(0, 120) : s;
    }
}
