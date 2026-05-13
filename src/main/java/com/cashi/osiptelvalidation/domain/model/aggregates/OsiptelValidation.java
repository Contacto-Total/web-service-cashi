package com.cashi.osiptelvalidation.domain.model.aggregates;

import com.cashi.osiptelvalidation.domain.model.valueobjects.DocumentType;
import com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus;
import com.cashi.shared.domain.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Aggregate Root de la validación Osiptel para un DOCUMENTO (no para un teléfono).
 *
 * El portal Osiptel funciona por documento: una consulta devuelve la lista de
 * líneas asociadas al documento, con el teléfono parcialmente enmascarado.
 *
 * Modelo:
 *  - Identidad: dni_hash (SHA-256 con sal global) + dni_type
 *  - Resultado: lines_json (lista de líneas devueltas por el portal)
 *  - El cross-matching contra metodos_contacto del cliente vive en
 *    `osiptel_phone_match` (entity separada, gestionada por el command service).
 *
 * Privacidad (Ley 29733):
 *  - NUNCA se persiste el nombre del titular ni el DNI plaintext.
 *  - Solo se guarda dni_hash y la lista de líneas (operador + prefijo + modalidad).
 */
@Entity
@Table(name = "osiptel_validation_log")
@Getter
@NoArgsConstructor
public class OsiptelValidation extends AggregateRoot {

    @Column(name = "dni_hash", length = 64, nullable = false)
    private String dniHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "dni_type", length = 20, nullable = false)
    private DocumentType dniType;

    @Column(name = "lines_json", columnDefinition = "TEXT")
    private String linesJson;

    @Column(name = "lines_count", nullable = false)
    private Short linesCount;

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

    @Column(name = "source_customer_id")
    private Long sourceCustomerId;

    @Column(name = "source_subportfolio_id")
    private Long sourceSubPortfolioId;

    @Column(name = "worker_id", length = 40)
    private String workerId;

    /**
     * Constructor de encolado. Estado inicial: PENDING.
     */
    public OsiptelValidation(String dniHash, DocumentType dniType,
                             Long sourceCustomerId, Long sourceSubPortfolioId) {
        super();
        this.dniHash = dniHash;
        this.dniType = dniType != null ? dniType : DocumentType.DNI;
        this.sourceCustomerId = sourceCustomerId;
        this.sourceSubPortfolioId = sourceSubPortfolioId;
        this.status = ValidationStatus.PENDING;
        this.attempts = 0;
        this.linesCount = 0;
        this.enqueuedAt = LocalDateTime.now();
    }

    // ============================
    // Transiciones de estado
    // ============================

    /** PENDING -> IN_PROGRESS. Reclamo del dispatcher antes de llamar al worker. */
    public void markInProgress(String workerId) {
        requireStatus(ValidationStatus.PENDING);
        this.status = ValidationStatus.IN_PROGRESS;
        this.workerId = workerId;
        this.startedAt = LocalDateTime.now();
        this.attempts = (short) (this.attempts + 1);
        updateTimestamp();
    }

    /**
     * IN_PROGRESS -> OK. Persiste la lista de líneas devueltas por el portal
     * y limpia errores previos.
     */
    public void recordOk(String linesJson, int linesCount, LocalDateTime cooldownUntil) {
        requireStatus(ValidationStatus.IN_PROGRESS);
        this.status = ValidationStatus.OK;
        this.linesJson = linesJson;
        this.linesCount = (short) linesCount;
        this.finishedAt = LocalDateTime.now();
        this.cooldownUntil = cooldownUntil;
        this.lastError = null;
        updateTimestamp();
    }

    /** IN_PROGRESS -> NOT_FOUND. El portal devolvió respuesta sin líneas. */
    public void recordNotFound(LocalDateTime cooldownUntil) {
        requireStatus(ValidationStatus.IN_PROGRESS);
        this.status = ValidationStatus.NOT_FOUND;
        this.linesCount = 0;
        this.linesJson = "[]";
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

    /** FAILED -> PENDING. Re-encolar tras cooldown. */
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

    /** Estado terminal por fatiga. */
    public void markExpired(String reason) {
        if (this.status == ValidationStatus.EXPIRED) {
            return;
        }
        this.status = ValidationStatus.EXPIRED;
        this.finishedAt = LocalDateTime.now();
        this.lastError = truncate(reason);
        updateTimestamp();
    }

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
