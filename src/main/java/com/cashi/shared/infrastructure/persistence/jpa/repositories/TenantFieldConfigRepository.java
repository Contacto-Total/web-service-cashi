package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.FieldDefinition;
import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.domain.model.entities.TenantFieldConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TenantFieldConfigRepository - Spring Data JPA repository for TenantFieldConfig entity
 */
@Repository
public interface TenantFieldConfigRepository extends JpaRepository<TenantFieldConfig, Long> {

    /**
     * Find config by tenant, portfolio and field definition
     */
    Optional<TenantFieldConfig> findByTenantAndPortfolioAndFieldDefinition(
        Tenant tenant, Portfolio portfolio, FieldDefinition fieldDefinition
    );

    /**
     * Find all enabled fields for tenant and portfolio
     */
    @Query("SELECT tfc FROM TenantFieldConfig tfc " +
           "WHERE tfc.tenant = :tenant AND tfc.portfolio = :portfolio " +
           "AND tfc.isEnabled = true ORDER BY tfc.displayOrder")
    List<TenantFieldConfig> findEnabledFieldsByTenantAndPortfolio(
        @Param("tenant") Tenant tenant, @Param("portfolio") Portfolio portfolio
    );

    /**
     * Find all enabled fields for tenant (no portfolio filter)
     */
    @Query("SELECT tfc FROM TenantFieldConfig tfc " +
           "WHERE tfc.tenant = :tenant AND tfc.portfolio IS NULL " +
           "AND tfc.isEnabled = true ORDER BY tfc.displayOrder")
    List<TenantFieldConfig> findEnabledFieldsByTenant(@Param("tenant") Tenant tenant);

    /**
     * Find all required fields for tenant and portfolio
     */
    @Query("SELECT tfc FROM TenantFieldConfig tfc " +
           "WHERE tfc.tenant = :tenant AND tfc.portfolio = :portfolio " +
           "AND tfc.isEnabled = true AND tfc.isRequired = true")
    List<TenantFieldConfig> findRequiredFieldsByTenantAndPortfolio(
        @Param("tenant") Tenant tenant, @Param("portfolio") Portfolio portfolio
    );

    /**
     * Find all field configs for tenant
     */
    List<TenantFieldConfig> findByTenant(Tenant tenant);

    /**
     * Find all field configs for portfolio
     */
    List<TenantFieldConfig> findByPortfolio(Portfolio portfolio);
}
