package com.cashi.systemconfiguration.domain.model.commands;

/**
 * Command to update an existing Tenant
 */
public record UpdateTenantCommand(
    String tenantName,
    String businessName,
    Boolean isActive
) {
}
