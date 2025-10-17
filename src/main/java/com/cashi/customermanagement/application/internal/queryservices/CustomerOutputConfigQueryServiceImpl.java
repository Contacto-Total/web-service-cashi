package com.cashi.customermanagement.application.internal.queryservices;

import com.cashi.customermanagement.domain.model.aggregates.CustomerOutputConfig;
import com.cashi.customermanagement.domain.model.queries.GetCustomerOutputConfigQuery;
import com.cashi.customermanagement.domain.services.CustomerOutputConfigQueryService;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerOutputConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementación del servicio de consultas para CustomerOutputConfig
 */
@Service
@RequiredArgsConstructor
public class CustomerOutputConfigQueryServiceImpl implements CustomerOutputConfigQueryService {

    private final CustomerOutputConfigRepository repository;

    /**
     * Obtiene configuración aplicable para tenant y portfolio
     *
     * LÓGICA DE FALLBACK:
     * 1. Si portfolioId != null → busca configuración específica del portfolio
     * 2. Si no encuentra específica → busca configuración general del tenant
     * 3. Si no encuentra ninguna → retorna Optional.empty() (frontend usa defaults)
     *
     * LOGS:
     * - Muestra qué configuración se está buscando
     * - Indica si encontró específica, general, o ninguna
     */
    @Override
    public Optional<CustomerOutputConfig> handle(GetCustomerOutputConfigQuery query) {
        System.out.println("🔍 Buscando configuración de outputs:");
        System.out.println("   → Tenant ID: " + query.tenantId());
        System.out.println("   → Portfolio ID: " + (query.portfolioId() != null ? query.portfolioId() : "null (buscar general)"));

        // Si hay portfolioId, buscar configuración específica primero
        if (query.portfolioId() != null) {
            Optional<CustomerOutputConfig> specificConfig =
                repository.findByTenantIdAndPortfolioId(query.tenantId(), query.portfolioId());

            if (specificConfig.isPresent()) {
                System.out.println("   ✅ Encontrada configuración específica para portfolio (ID: " + specificConfig.get().getId() + ")");
                return specificConfig;
            } else {
                System.out.println("   ⚠️ No hay configuración específica para portfolio, buscando general...");
            }
        }

        // Buscar configuración general del tenant
        Optional<CustomerOutputConfig> generalConfig =
            repository.findByTenantIdAndPortfolioIdIsNull(query.tenantId());

        if (generalConfig.isPresent()) {
            System.out.println("   ✅ Encontrada configuración general del tenant (ID: " + generalConfig.get().getId() + ")");
            return generalConfig;
        } else {
            System.out.println("   ❌ No hay configuración (ni específica ni general). Frontend usará valores por defecto.");
            return Optional.empty();
        }
    }
}
