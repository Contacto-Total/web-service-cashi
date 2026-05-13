package com.cashi.osiptelvalidation.domain.model.commands;

import com.cashi.osiptelvalidation.domain.model.valueobjects.OsiptelLine;

import java.util.List;

/**
 * Comando: registrar el resultado entregado por el worker para una validación
 * en IN_PROGRESS.
 *
 * resultStatus mapea 1:1 al output del worker (OK | NOT_FOUND | CAPTCHA_FAIL |
 * BANNED | ERROR). Si es OK, viene la lista de líneas; el command service
 * persistirá lines_json + creará osiptel_phone_match por cada teléfono del
 * cliente cruzando los prefijos.
 */
public record RecordValidationResultCommand(
        Long validationId,
        String workerId,
        String resultStatus,        // OK | NOT_FOUND | CAPTCHA_FAIL | BANNED | ERROR
        List<OsiptelLine> lines,    // null si resultStatus != OK
        Integer latencyMs,
        Integer captchaAttempts,
        Integer httpStatus,
        String errorDetail
) {}
