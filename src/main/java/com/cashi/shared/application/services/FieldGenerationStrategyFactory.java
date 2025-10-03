package com.cashi.shared.application.services;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.ProcessingStrategy;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.domain.services.FieldGenerationStrategy;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.ProcessingStrategyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FieldGenerationStrategyFactory - Factory pattern for creating field generation strategies
 * Determines which fields to display based on tenant/portfolio/management type
 */
@Service
public class FieldGenerationStrategyFactory {

    private final ProcessingStrategyRepository strategyRepository;
    private final Map<String, FieldGenerationStrategy> strategyCache = new ConcurrentHashMap<>();

    public FieldGenerationStrategyFactory(ProcessingStrategyRepository strategyRepository) {
        this.strategyRepository = strategyRepository;
    }

    /**
     * Gets field generation strategy for tenant and portfolio
     */
    public FieldGenerationStrategy getFieldGenerationStrategy(Tenant tenant, Portfolio portfolio) {
        String cacheKey = getCacheKey(tenant, portfolio);

        return strategyCache.computeIfAbsent(cacheKey, k -> {
            // Try to find portfolio-specific strategy first
            if (portfolio != null) {
                var portfolioStrategy = strategyRepository.findByTenantAndPortfolioAndStrategyType(
                    tenant, portfolio, ProcessingStrategy.StrategyType.FIELD_GENERATION_STRATEGY
                );
                if (portfolioStrategy.isPresent() && portfolioStrategy.get().getIsActive()) {
                    return instantiateStrategy(portfolioStrategy.get());
                }
            }

            // Fall back to tenant-level strategy
            var tenantStrategy = strategyRepository.findByTenantAndPortfolioIsNullAndStrategyType(
                tenant, ProcessingStrategy.StrategyType.FIELD_GENERATION_STRATEGY
            );
            if (tenantStrategy.isPresent() && tenantStrategy.get().getIsActive()) {
                return instantiateStrategy(tenantStrategy.get());
            }

            // Return default strategy if no configuration found
            return new DefaultFieldGenerationStrategy();
        });
    }

    /**
     * Clears strategy cache
     */
    public void clearCache(Tenant tenant, Portfolio portfolio) {
        String cacheKey = getCacheKey(tenant, portfolio);
        strategyCache.remove(cacheKey);
    }

    public void clearAllCache() {
        strategyCache.clear();
    }

    private String getCacheKey(Tenant tenant, Portfolio portfolio) {
        if (portfolio != null) {
            return tenant.getTenantCode() + ":" + portfolio.getPortfolioCode();
        }
        return tenant.getTenantCode() + ":default";
    }

    private FieldGenerationStrategy instantiateStrategy(ProcessingStrategy strategyConfig) {
        try {
            String className = strategyConfig.getStrategyImplementation();
            Class<?> strategyClass = Class.forName(className);
            return (FieldGenerationStrategy) strategyClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate field generation strategy: " +
                strategyConfig.getStrategyImplementation(), e);
        }
    }

    /**
     * Default field generation strategy
     */
    private static class DefaultFieldGenerationStrategy implements FieldGenerationStrategy {

        @Override
        public List<String> generateRequiredFields(Map<String, Object> context) {
            // Default: no additional required fields
            return new ArrayList<>();
        }

        @Override
        public List<String> generateOptionalFields(Map<String, Object> context) {
            // Default: no additional optional fields
            return new ArrayList<>();
        }

        @Override
        public String getStrategyName() {
            return "DefaultFieldGenerationStrategy";
        }

        @Override
        public boolean appliesTo(String tenantCode, String portfolioCode, String managementTypeCode) {
            return true;
        }
    }
}
