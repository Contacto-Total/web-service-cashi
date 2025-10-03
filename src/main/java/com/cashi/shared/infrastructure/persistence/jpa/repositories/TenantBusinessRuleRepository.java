package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.domain.model.entities.TenantBusinessRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TenantBusinessRuleRepository - Repository for tenant business rules
 */
@Repository
public interface TenantBusinessRuleRepository extends JpaRepository<TenantBusinessRule, Long> {

    /**
     * Find by tenant, portfolio and rule code
     */
    Optional<TenantBusinessRule> findByTenantAndPortfolioAndRuleCode(
        Tenant tenant, Portfolio portfolio, String ruleCode
    );

    /**
     * Find all active rules for tenant and portfolio
     */
    @Query("SELECT r FROM TenantBusinessRule r " +
           "WHERE r.tenant = :tenant AND r.portfolio = :portfolio " +
           "AND r.isActive = true ORDER BY r.priority")
    List<TenantBusinessRule> findActiveByTenantAndPortfolio(
        @Param("tenant") Tenant tenant, @Param("portfolio") Portfolio portfolio
    );

    /**
     * Find all active rules for tenant (no portfolio)
     */
    @Query("SELECT r FROM TenantBusinessRule r " +
           "WHERE r.tenant = :tenant AND r.portfolio IS NULL " +
           "AND r.isActive = true ORDER BY r.priority")
    List<TenantBusinessRule> findActiveByTenant(@Param("tenant") Tenant tenant);

    /**
     * Find rules by type
     */
    @Query("SELECT r FROM TenantBusinessRule r " +
           "WHERE r.tenant = :tenant AND r.portfolio = :portfolio " +
           "AND r.ruleType = :ruleType AND r.isActive = true ORDER BY r.priority")
    List<TenantBusinessRule> findByTenantAndPortfolioAndRuleType(
        @Param("tenant") Tenant tenant,
        @Param("portfolio") Portfolio portfolio,
        @Param("ruleType") TenantBusinessRule.RuleType ruleType
    );

    /**
     * Find rules by category
     */
    List<TenantBusinessRule> findByTenantAndRuleCategoryAndIsActive(
        Tenant tenant, String ruleCategory, Boolean isActive
    );
}
