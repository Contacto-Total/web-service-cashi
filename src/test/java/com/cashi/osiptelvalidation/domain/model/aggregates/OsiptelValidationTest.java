package com.cashi.osiptelvalidation.domain.model.aggregates;

import com.cashi.osiptelvalidation.domain.model.valueobjects.DocumentType;
import com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la máquina de estados del aggregate (post-pivot).
 */
class OsiptelValidationTest {

    @Test
    void recienCreadoEstaPendingConCeroIntentosYLineas() {
        OsiptelValidation v = new OsiptelValidation("hash-123", DocumentType.DNI, 100L, 200L);

        assertEquals(ValidationStatus.PENDING, v.getStatus());
        assertEquals(DocumentType.DNI, v.getDniType());
        assertEquals(0, v.getAttempts().intValue());
        assertEquals(0, v.getLinesCount().intValue());
        assertNotNull(v.getEnqueuedAt());
        assertNull(v.getStartedAt());
        assertNull(v.getFinishedAt());
    }

    @Test
    void markInProgressIncrementaAttempts() {
        OsiptelValidation v = new OsiptelValidation("hash-123", DocumentType.DNI, 100L, 200L);

        v.markInProgress("worker-1");

        assertEquals(ValidationStatus.IN_PROGRESS, v.getStatus());
        assertEquals(1, v.getAttempts().intValue());
        assertEquals("worker-1", v.getWorkerId());
        assertNotNull(v.getStartedAt());
    }

    @Test
    void recordOkSetLinesJsonYCount() {
        OsiptelValidation v = new OsiptelValidation("hash-123", DocumentType.DNI, 100L, 200L);
        v.markInProgress("worker-1");

        LocalDateTime cooldown = LocalDateTime.now().plusDays(90);
        String json = "[{\"phonePrefix\":\"97851\",\"operator\":\"MOVISTAR\",\"modality\":\"CONTROL\"}]";
        v.recordOk(json, 1, cooldown);

        assertEquals(ValidationStatus.OK, v.getStatus());
        assertEquals(json, v.getLinesJson());
        assertEquals(1, v.getLinesCount().intValue());
        assertEquals(cooldown, v.getCooldownUntil());
        assertNotNull(v.getFinishedAt());
    }

    @Test
    void noPermiteTransicionDePendingDirectoAOk() {
        OsiptelValidation v = new OsiptelValidation("hash-123", DocumentType.DNI, 100L, 200L);

        assertThrows(IllegalStateException.class,
                () -> v.recordOk("[]", 0, LocalDateTime.now()));
    }

    @Test
    void recordNotFoundLimpiaLineasYMarcaFinishedAt() {
        OsiptelValidation v = new OsiptelValidation("hash-123", DocumentType.DNI, 100L, 200L);
        v.markInProgress("worker-1");
        v.recordNotFound(LocalDateTime.now().plusDays(30));

        assertEquals(ValidationStatus.NOT_FOUND, v.getStatus());
        assertEquals(0, v.getLinesCount().intValue());
        assertEquals("[]", v.getLinesJson());
        assertNotNull(v.getFinishedAt());
    }

    @Test
    void requeueAfterFailureVuelveAPending() {
        OsiptelValidation v = new OsiptelValidation("hash-123", DocumentType.DNI, 100L, 200L);
        v.markInProgress("worker-1");
        v.recordFailed("CAPTCHA_FAIL", LocalDateTime.now().plusDays(1));
        assertEquals(ValidationStatus.FAILED, v.getStatus());

        v.requeueAfterFailure();

        assertEquals(ValidationStatus.PENDING, v.getStatus());
        assertNull(v.getStartedAt());
        assertNull(v.getFinishedAt());
        assertNull(v.getWorkerId());
        assertEquals(1, v.getAttempts().intValue());
    }

    @Test
    void reclaimStuckDevuelveInProgressAPending() {
        OsiptelValidation v = new OsiptelValidation("hash-123", DocumentType.DNI, 100L, 200L);
        v.markInProgress("worker-dead");

        v.reclaimStuck();

        assertEquals(ValidationStatus.PENDING, v.getStatus());
        assertNull(v.getStartedAt());
        assertNull(v.getWorkerId());
        assertEquals(1, v.getAttempts().intValue());
    }

    @Test
    void markExpiredEsIdempotente() {
        OsiptelValidation v = new OsiptelValidation("hash-123", DocumentType.DNI, 100L, 200L);
        v.markExpired("max-attempts");
        LocalDateTime firstFinish = v.getFinishedAt();

        v.markExpired("repeat");

        assertEquals(ValidationStatus.EXPIRED, v.getStatus());
        assertEquals(firstFinish, v.getFinishedAt());
    }

    @Test
    void isInCooldownConsideraFechaActual() {
        OsiptelValidation v = new OsiptelValidation("hash-123", DocumentType.DNI, 100L, 200L);
        v.markInProgress("worker-1");
        v.recordOk("[]", 0, LocalDateTime.now().plusDays(90));

        assertTrue(v.isInCooldown(LocalDateTime.now()));
        assertFalse(v.isInCooldown(LocalDateTime.now().plusDays(91)));
    }

    @Test
    void canBeRetriedRespetaMaxAttempts() {
        OsiptelValidation v = new OsiptelValidation("hash-123", DocumentType.DNI, 100L, 200L);
        v.markInProgress("worker-1");
        assertTrue(v.canBeRetried(3));

        v.markExpired("max-attempts");
        assertFalse(v.canBeRetried(3));
    }
}
