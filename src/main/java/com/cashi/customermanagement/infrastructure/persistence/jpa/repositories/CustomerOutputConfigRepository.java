package com.cashi.customermanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.customermanagement.domain.model.aggregates.CustomerOutputConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para CustomerOutputConfig
 *
 * CONSULTAS PRINCIPALES:
 * 1. Buscar por tenant y portfolio específico
 * 2. Buscar por tenant (sin portfolio = configuración general)
 * 3. Buscar configuración aplicable (con fallback a general si no existe específica)
 */
@Repository
public interface CustomerOutputConfigRepository extends JpaRepository<CustomerOutputConfig, Long> {

    /**
     * Busca configuración específica por tenant y portfolio
     *
     * USO: Cuando el usuario está trabajando en una cartera específica
     *
     * @param tenantId ID del tenant
     * @param portfolioId ID del portfolio
     * @return Configuración específica si existe
     */
    Optional<CustomerOutputConfig> findByTenantIdAndPortfolioId(Long tenantId, Long portfolioId);

    /**
     * Busca configuración general del tenant (sin portfolio específico)
     *
     * USO: Configuración por defecto para todo el tenant
     *
     * @param tenantId ID del tenant
     * @return Configuración general del tenant
     */
    Optional<CustomerOutputConfig> findByTenantIdAndPortfolioIdIsNull(Long tenantId);

    /**
     * Busca todas las configuraciones de un tenant (general + específicas por portfolio)
     *
     * USO: Para listar todas las configuraciones disponibles
     *
     * @param tenantId ID del tenant
     * @return Lista de configuraciones del tenant
     */
    java.util.List<CustomerOutputConfig> findByTenantId(Long tenantId);
}
