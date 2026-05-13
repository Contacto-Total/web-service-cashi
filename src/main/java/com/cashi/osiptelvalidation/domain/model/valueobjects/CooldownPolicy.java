package com.cashi.osiptelvalidation.domain.model.valueobjects;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Política de cooldown: cuánto tiempo no re-validar un número tras cierto resultado.
 * Centraliza las constantes para que sean configurables desde OsiptelProperties.
 */
public final class CooldownPolicy {

    private final Duration okDuration;
    private final Duration notFoundDuration;
    private final Duration failedDuration;

    public CooldownPolicy(Duration okDuration, Duration notFoundDuration, Duration failedDuration) {
        this.okDuration = okDuration;
        this.notFoundDuration = notFoundDuration;
        this.failedDuration = failedDuration;
    }

    /** Defaults: OK=90d, NOT_FOUND=30d, FAILED=1d. */
    public static CooldownPolicy defaults() {
        return new CooldownPolicy(
                Duration.ofDays(90),
                Duration.ofDays(30),
                Duration.ofDays(1)
        );
    }

    public LocalDateTime nextCooldownFor(ValidationStatus status, LocalDateTime now) {
        return switch (status) {
            case OK -> now.plus(okDuration);
            case NOT_FOUND -> now.plus(notFoundDuration);
            case FAILED -> now.plus(failedDuration);
            default -> null;
        };
    }
}
