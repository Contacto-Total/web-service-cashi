package com.cashi.osiptelvalidation.domain.model.commands;

import com.cashi.osiptelvalidation.domain.model.valueobjects.OperatorCode;

/**
 * Comando: registrar el resultado entregado por el worker para una validación en IN_PROGRESS.
 *
 * resultStatus mapea 1:1 al output del worker (OK | NOT_FOUND | CAPTCHA_FAIL | BANNED | ERROR).
 * El command service traduce a la transición correspondiente del aggregate.
 */
public record RecordValidationResultCommand(
        Long validationId,
        String workerId,
        String resultStatus,   // OK | NOT_FOUND | CAPTCHA_FAIL | BANNED | ERROR
        OperatorCode operator,
        Boolean dniMatch,
        Integer latencyMs,
        Integer captchaAttempts,
        Integer httpStatus,
        String errorDetail
) {}
