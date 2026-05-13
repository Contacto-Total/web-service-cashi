package com.cashi.osiptelvalidation.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Resultado que el Electron app envía de vuelta tras procesar un job.
 *
 * lines solo está presente si resultStatus == "OK". Cada elemento es
 * {phonePrefix:string, operator:string, modality:string|null}.
 */
public record CallbackRequest(
        @NotNull Long validationId,
        @NotBlank String workerId,
        @NotBlank String resultStatus,    // OK | NOT_FOUND | CAPTCHA_FAIL | BANNED | ERROR
        List<LineItem> lines,
        Integer latencyMs,
        Integer captchaAttempts,
        Integer httpStatus,
        String errorDetail
) {
    public record LineItem(String phonePrefix, String operator, String modality) {}
}
