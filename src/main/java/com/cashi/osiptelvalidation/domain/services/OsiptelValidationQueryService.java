package com.cashi.osiptelvalidation.domain.services;

import com.cashi.osiptelvalidation.domain.model.aggregates.OsiptelValidation;
import com.cashi.osiptelvalidation.domain.model.queries.GetValidationByPhoneQuery;
import com.cashi.osiptelvalidation.domain.model.queries.GetValidationMetricsByPortfolioQuery;

import java.util.Map;
import java.util.Optional;

public interface OsiptelValidationQueryService {

    /** Última validación finalizada (o pendiente si no hay finalizada) para un número. */
    Optional<OsiptelValidation> findLatestByPhone(GetValidationByPhoneQuery query);

    /**
     * Agregados por subcartera y rango.
     * Keys del map: total, ok, notFound, failed, expired, pending, inProgress,
     *               dniMatchTrue, dniMatchFalse, byOperator (Map<String,Long>).
     */
    Map<String, Object> getMetrics(GetValidationMetricsByPortfolioQuery query);
}
