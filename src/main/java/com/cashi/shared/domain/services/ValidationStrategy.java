package com.cashi.shared.domain.services;

import java.util.Map;

/**
 * ValidationStrategy - Strategy pattern interface for tenant-specific validation
 * Implementations provide custom validation logic per tenant/portfolio
 */
public interface ValidationStrategy {

    /**
     * Validates field values based on tenant-specific rules
     *
     * @param fieldValues Map of field codes to values
     * @return ValidationResult containing success status and error messages
     */
    ValidationResult validate(Map<String, Object> fieldValues);

    /**
     * Gets the strategy name
     */
    String getStrategyName();

    /**
     * Checks if this strategy applies to the given context
     */
    boolean appliesTo(String tenantCode, String portfolioCode);
}
