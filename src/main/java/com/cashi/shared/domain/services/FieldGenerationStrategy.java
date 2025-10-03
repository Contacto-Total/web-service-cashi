package com.cashi.shared.domain.services;

import java.util.List;
import java.util.Map;

/**
 * FieldGenerationStrategy - Strategy pattern for dynamic field generation
 * Implementations determine which fields to show based on tenant/portfolio config
 */
public interface FieldGenerationStrategy {

    /**
     * Generates list of field definitions based on context
     *
     * @param context Map containing tenant, portfolio, management type, etc.
     * @return List of field codes that should be displayed/required
     */
    List<String> generateRequiredFields(Map<String, Object> context);

    /**
     * Generates list of optional field definitions based on context
     */
    List<String> generateOptionalFields(Map<String, Object> context);

    /**
     * Gets the strategy name
     */
    String getStrategyName();

    /**
     * Checks if this strategy applies to the given context
     */
    boolean appliesTo(String tenantCode, String portfolioCode, String managementTypeCode);
}
