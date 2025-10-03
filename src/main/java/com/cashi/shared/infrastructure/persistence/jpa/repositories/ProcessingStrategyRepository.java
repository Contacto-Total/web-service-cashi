package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.ProcessingStrategy;
import com.cashi.shared.domain.model.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ProcessingStrategyRepository - Repository for processing strategies
 */
@Repository
public interface ProcessingStrategyRepository extends JpaRepository<ProcessingStrategy, Long> {

    /**
     * Find by tenant, portfolio and strategy type
     */
    Optional<ProcessingStrategy> findByTenantAndPortfolioAndStrategyType(
        Tenant tenant, Portfolio portfolio, ProcessingStrategy.StrategyType strategyType
    );

    /**
     * Find by tenant and strategy type (no portfolio)
     */
    Optional<ProcessingStrategy> findByTenantAndPortfolioIsNullAndStrategyType(
        Tenant tenant, ProcessingStrategy.StrategyType strategyType
    );

    /**
     * Find all active strategies for tenant and portfolio
     */
    @Query("SELECT s FROM ProcessingStrategy s " +
           "WHERE s.tenant = :tenant AND s.portfolio = :portfolio " +
           "AND s.isActive = true ORDER BY s.priority")
    List<ProcessingStrategy> findActiveByTenantAndPortfolio(
        @Param("tenant") Tenant tenant, @Param("portfolio") Portfolio portfolio
    );

    /**
     * Find all active strategies for tenant (no portfolio)
     */
    @Query("SELECT s FROM ProcessingStrategy s " +
           "WHERE s.tenant = :tenant AND s.portfolio IS NULL " +
           "AND s.isActive = true ORDER BY s.priority")
    List<ProcessingStrategy> findActiveByTenant(@Param("tenant") Tenant tenant);

    /**
     * Find all strategies by type
     */
    List<ProcessingStrategy> findByStrategyTypeAndIsActive(
        ProcessingStrategy.StrategyType strategyType, Boolean isActive
    );
}
