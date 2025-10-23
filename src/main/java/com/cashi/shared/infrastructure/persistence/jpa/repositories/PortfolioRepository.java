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
public interface PortfolioRepository extends JpaRepository<Portfolio, Integer> {

    /**
     * Find portfolio by tenant and code
     */
    Optional<Portfolio> findByTenantAndPortfolioCode(Tenant tenant, String portfolioCode);

    /**
     * Find all portfolios for a tenant
     */
    List<Portfolio> findByTenant(Tenant tenant);

    /**
     * Check if portfolio code exists for tenant
     */
    boolean existsByTenantAndPortfolioCode(Tenant tenant, String portfolioCode);

    /**
     * Count portfolios for a tenant
     */
    long countByTenant(Tenant tenant);
}
