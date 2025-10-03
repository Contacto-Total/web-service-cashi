package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PortfolioRepository - Spring Data JPA repository for Portfolio entity
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * Find portfolio by tenant and code
     */
    Optional<Portfolio> findByTenantAndPortfolioCode(Tenant tenant, String portfolioCode);

    /**
     * Find all portfolios for a tenant
     */
    List<Portfolio> findByTenantAndIsActive(Tenant tenant, Boolean isActive);

    /**
     * Find all portfolios for a tenant ordered by hierarchy
     */
    @Query("SELECT p FROM Portfolio p WHERE p.tenant = :tenant AND p.isActive = true ORDER BY p.hierarchyLevel, p.portfolioName")
    List<Portfolio> findByTenantOrderedByHierarchy(@Param("tenant") Tenant tenant);

    /**
     * Find root portfolios (no parent)
     */
    @Query("SELECT p FROM Portfolio p WHERE p.tenant = :tenant AND p.parentPortfolio IS NULL AND p.isActive = true")
    List<Portfolio> findRootPortfoliosByTenant(@Param("tenant") Tenant tenant);

    /**
     * Find child portfolios
     */
    List<Portfolio> findByParentPortfolioAndIsActive(Portfolio parentPortfolio, Boolean isActive);

    /**
     * Find portfolios by type
     */
    List<Portfolio> findByTenantAndPortfolioTypeAndIsActive(
        Tenant tenant, Portfolio.PortfolioType portfolioType, Boolean isActive
    );

    /**
     * Check if portfolio code exists for tenant
     */
    boolean existsByTenantAndPortfolioCode(Tenant tenant, String portfolioCode);
}
