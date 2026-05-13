package com.cashi.osiptelvalidation.domain.model.valueobjects;

/**
 * Estados del ciclo de vida de una validación Osiptel.
 *
 * Flujo normal: PENDING -> IN_PROGRESS -> (OK | NOT_FOUND | FAILED)
 * Estado terminal por fatiga: EXPIRED (cuando attempts supera el máximo configurado).
 */
public enum ValidationStatus {
    /** Encolado, en espera de ser procesado por el worker. */
    PENDING,
    /** Reclamado por el dispatcher, en envío al worker. */
    IN_PROGRESS,
    /** Validación exitosa (titular o no titular, ambos casos están en OK con dni_match). */
    OK,
    /** El portal no encontró el número (prepago no registrado, porteo reciente, etc.). */
    NOT_FOUND,
    /** Falla técnica reintentable (captcha, timeout, parser, etc.). */
    FAILED,
    /** Demasiados intentos sin éxito; no se reintenta. */
    EXPIRED;

    public boolean isTerminal() {
        return this == OK || this == NOT_FOUND || this == EXPIRED;
    }

    public boolean isActive() {
        return this == PENDING || this == IN_PROGRESS;
    }
}
