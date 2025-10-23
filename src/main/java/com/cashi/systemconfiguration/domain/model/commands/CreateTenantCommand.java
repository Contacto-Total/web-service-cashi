package com.cashi.systemconfiguration.domain.model.commands;

/**
 * Command to create a new Tenant
 */
public record CreateTenantCommand(
    String tenantCode,
    String tenantName,
    String businessName,
    String taxId,
    String countryCode,
    String timezone,
    Integer maxUsers,
    Integer maxConcurrentSessions,
    String subscriptionTier
) {
}
