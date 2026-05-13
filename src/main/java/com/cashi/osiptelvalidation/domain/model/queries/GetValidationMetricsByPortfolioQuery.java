package com.cashi.osiptelvalidation.domain.model.queries;

import java.time.LocalDateTime;

/**
 * Query: agregados de validaciones por subcartera y rango temporal.
 * subPortfolioId opcional - si es null, devuelve totales globales.
 */
public record GetValidationMetricsByPortfolioQuery(
        Long subPortfolioId,
        LocalDateTime from,
        LocalDateTime to
) {}
