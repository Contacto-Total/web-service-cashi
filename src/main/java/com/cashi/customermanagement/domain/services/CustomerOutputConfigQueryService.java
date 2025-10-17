package com.cashi.customermanagement.domain.services;

import com.cashi.customermanagement.domain.model.aggregates.CustomerOutputConfig;
import com.cashi.customermanagement.domain.model.queries.GetCustomerOutputConfigQuery;

import java.util.Optional;

/**
 * Servicio de consultas para CustomerOutputConfig
 */
public interface CustomerOutputConfigQueryService {
    /**
     * Obtiene configuración aplicable para tenant y portfolio
     *
     * LÓGICA:
     * 1. Si portfolioId != null → busca configuración específica
     * 2. Si no encuentra → busca configuración general del tenant
     * 3. Si no encuentra ninguna → retorna Optional.empty()
     */
    Optional<CustomerOutputConfig> handle(GetCustomerOutputConfigQuery query);
}
