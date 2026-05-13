package com.cashi.osiptelvalidation.interfaces.rest.resources;

import java.time.LocalDateTime;

/**
 * Response: GET /api/v1/osiptel/validations/{phone}
 *
 * NO incluye DNI plaintext, dni_hash, ni nombre del titular - solo el match boolean.
 */
public record ValidationStatusResource(
        String phone,
        String status,
        Boolean dniMatch,
        String operator,
        String modality,
        LocalDateTime checkedAt,
        LocalDateTime cooldownUntil,
        Integer attempts
) {}
