package com.cashi.osiptelvalidation.domain.services;

import com.cashi.osiptelvalidation.domain.model.queries.GetValidationByPhoneQuery;
import com.cashi.osiptelvalidation.domain.model.queries.GetValidationMetricsByPortfolioQuery;
import com.cashi.osiptelvalidation.domain.model.valueobjects.OperatorCode;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public interface OsiptelValidationQueryService {

    /**
     * Última validación conocida que tocó este teléfono (vía osiptel_phone_match).
     * Combina datos del match (dni_match, operador) con datos del aggregate
     * padre (status, cooldown).
     */
    Optional<PhoneValidationView> findLatestByPhone(GetValidationByPhoneQuery query);

    Map<String, Object> getMetrics(GetValidationMetricsByPortfolioQuery query);

    /**
     * Vista denormalizada: lo que el frontend necesita renderizar el badge.
     * Safe para exponer (no incluye DNI plaintext ni nombre).
     */
    record PhoneValidationView(
            String phone,
            String status,
            Boolean dniMatch,
            OperatorCode operator,
            String modality,
            LocalDateTime checkedAt,
            LocalDateTime cooldownUntil,
            Integer attempts
    ) {}
}
