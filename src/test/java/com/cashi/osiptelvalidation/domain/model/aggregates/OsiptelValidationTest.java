package com.cashi.osiptelvalidation.domain.model.aggregates;

import com.cashi.osiptelvalidation.domain.model.valueobjects.OperatorCode;
import com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la máquina de estados del aggregate.
 * Garantiza que las transiciones son las únicas permitidas.
 */
class OsiptelValidationTest {

    @Test
    void recienCreadoEstaPendingConCeroIntentos() {
        OsiptelValidation v = new OsiptelValidation("987654321", "hash-123", 1L, 100L);

        assertEquals(ValidationStatus.PENDING, v.getStatus());
        assertEquals(0, v.getAttempts().intValue());
        assertNotNull(v.getEnqueuedAt());
        assertNull(v.getStartedAt());
        assertNull(v.getFinishedAt());
    }

    @Test
    void markInProgressIncrementaAttempts() {
        OsiptelValidation v = new OsiptelValidation("987654321", "hash-123", 1L, 100L);

        v.markInProgress("worker-1");

        assertEquals(ValidationStatus.IN_PROGRESS, v.getStatus());
        assertEquals(1, v.getAttempts().intValue());
        assertEquals("worker-1", v.getWorkerId());
        assertNotNull(v.getStartedAt());
    }

    @Test
    void recordOkSetOperatorYDniMatch() {
        OsiptelValidation v = new OsiptelValidation("987654321", "hash-123", 1L, 100L);
        v.markInProgress("worker-1");

        LocalDateTime cooldown = LocalDateTime.now().plusDays(90);
        v.recordOk(OperatorCode.CLARO, true, cooldown);

        assertEquals(ValidationStatus.OK, v.getStatus());
        assertEquals(OperatorCode.CLARO, v.getOperator());
        assertEquals(Boolean.TRUE, v.getDniMatch());
        assertEquals(cooldown, v.getCooldownUntil());
        assertNotNull(v.getFinishedAt());
    }

    @Test
    void noPermiteTransicionDePendingDirectoAOk() {
        OsiptelValidation v = new OsiptelValidation("987654321", "hash-123", 1L, 100L);

        assertThrows(IllegalStateException.class,
                () -> v.recordOk(OperatorCode.CLARO, true, LocalDateTime.now()));
    }

    @Test
    void requeueAfterFailureVuelveAPending() {
        OsiptelValidation v = new OsiptelValidation("987654321", "hash-123", 1L, 100L);
        v.markInProgress("worker-1");
        v.recordFailed("CAPTCHA_FAIL", LocalDateTime.now().plusDays(1));
        assertEquals(ValidationStatus.FAILED, v.getStatus());

        v.requeueAfterFailure();

        assertEquals(ValidationStatus.PENDING, v.getStatus());
        assertNull(v.getStartedAt());
        assertNull(v.getFinishedAt());
        assertNull(v.getWorkerId());
        // attempts NO se resetea - sigue siendo 1
        assertEquals(1, v.getAttempts().intValue());
    }

    @Test
    void reclaimStuckDevuelveInProgressAPending() {
        OsiptelValidation v = new OsiptelValidation("987654321", "hash-123", 1L, 100L);
        v.markInProgress("worker-dead");

        v.reclaimStuck();

        assertEquals(ValidationStatus.PENDING, v.getStatus());
        assertNull(v.getStartedAt());
        assertNull(v.getWorkerId());
        // attempts ya se incrementó en markInProgress
        assertEquals(1, v.getAttempts().intValue());
    }

    @Test
    void markExpiredEsIdempotente() {
        OsiptelValidation v = new OsiptelValidation("987654321", "hash-123", 1L, 100L);
        v.markExpired("max-attempts");
        LocalDateTime firstFinish = v.getFinishedAt();

        // Segunda llamada no debe cambiar el timestamp
        v.markExpired("repeat");

        assertEquals(ValidationStatus.EXPIRED, v.getStatus());
        assertEquals(firstFinish, v.getFinishedAt());
    }

    @Test
    void isInCooldownConsideraFechaActual() {
        OsiptelValidation v = new OsiptelValidation("987654321", "hash-123", 1L, 100L);
        v.markInProgress("worker-1");
        v.recordOk(OperatorCode.CLARO, true, LocalDateTime.now().plusDays(90));

        assertTrue(v.isInCooldown(LocalDateTime.now()));
        assertFalse(v.isInCooldown(LocalDateTime.now().plusDays(91)));
    }

    @Test
    void canBeRetriedRespetaMaxAttempts() {
        OsiptelValidation v = new OsiptelValidation("987654321", "hash-123", 1L, 100L);
        v.markInProgress("worker-1");
        // attempts=1
        assertTrue(v.canBeRetried(3));

        v.markExpired("max-attempts");
        assertFalse(v.canBeRetried(3));
    }
}
