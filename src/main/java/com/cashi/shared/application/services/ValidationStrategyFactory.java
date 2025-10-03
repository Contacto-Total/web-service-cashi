package com.cashi.shared.application.services;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.ProcessingStrategy;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.domain.services.ValidationStrategy;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.ProcessingStrategyRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ValidationStrategyFactory - Factory pattern implementation for creating validation strategies
 * Loads and caches strategy implementations based on tenant/portfolio configuration
 */
@Service
public class ValidationStrategyFactory {

    private final ProcessingStrategyRepository strategyRepository;
    private final Map<String, ValidationStrategy> strategyCache = new ConcurrentHashMap<>();

    public ValidationStrategyFactory(ProcessingStrategyRepository strategyRepository) {
        this.strategyRepository = strategyRepository;
    }

    /**
     * Gets validation strategy for tenant and portfolio
     * First checks portfolio-specific strategy, then falls back to tenant-level
     */
    public ValidationStrategy getValidationStrategy(Tenant tenant, Portfolio portfolio) {
        String cacheKey = getCacheKey(tenant, portfolio);

        return strategyCache.computeIfAbsent(cacheKey, k -> {
            // Try to find portfolio-specific strategy first
            if (portfolio != null) {
                var portfolioStrategy = strategyRepository.findByTenantAndPortfolioAndStrategyType(
                    tenant, portfolio, ProcessingStrategy.StrategyType.VALIDATION_STRATEGY
                );
                if (portfolioStrategy.isPresent() && portfolioStrategy.get().getIsActive()) {
                    return instantiateStrategy(portfolioStrategy.get());
                }
            }

            // Fall back to tenant-level strategy
            var tenantStrategy = strategyRepository.findByTenantAndPortfolioIsNullAndStrategyType(
                tenant, ProcessingStrategy.StrategyType.VALIDATION_STRATEGY
            );
            if (tenantStrategy.isPresent() && tenantStrategy.get().getIsActive()) {
                return instantiateStrategy(tenantStrategy.get());
            }

            // Return default strategy if no configuration found
            return new DefaultValidationStrategy();
        });
    }

    /**
     * Clears strategy cache for a specific tenant/portfolio
     */
    public void clearCache(Tenant tenant, Portfolio portfolio) {
        String cacheKey = getCacheKey(tenant, portfolio);
        strategyCache.remove(cacheKey);
    }

    /**
     * Clears entire strategy cache
     */
    public void clearAllCache() {
        strategyCache.clear();
    }

    private String getCacheKey(Tenant tenant, Portfolio portfolio) {
        if (portfolio != null) {
            return tenant.getTenantCode() + ":" + portfolio.getPortfolioCode();
        }
        return tenant.getTenantCode() + ":default";
    }

    private ValidationStrategy instantiateStrategy(ProcessingStrategy strategyConfig) {
        try {
            String className = strategyConfig.getStrategyImplementation();
            Class<?> strategyClass = Class.forName(className);
            return (ValidationStrategy) strategyClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate validation strategy: " +
                strategyConfig.getStrategyImplementation(), e);
        }
    }

    /**
     * Default validation strategy used when no specific configuration exists
     */
    private static class DefaultValidationStrategy implements ValidationStrategy {

        @Override
        public com.cashi.shared.domain.services.ValidationResult validate(Map<String, Object> fieldValues) {
            // Default implementation - no custom validation
            return com.cashi.shared.domain.services.ValidationResult.success();
        }

        @Override
        public String getStrategyName() {
            return "DefaultValidationStrategy";
        }

        @Override
        public boolean appliesTo(String tenantCode, String portfolioCode) {
            return true;
        }
    }
}
