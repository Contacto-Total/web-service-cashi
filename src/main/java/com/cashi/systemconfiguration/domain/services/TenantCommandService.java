package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.systemconfiguration.domain.model.commands.CreateTenantCommand;
import com.cashi.systemconfiguration.domain.model.commands.UpdateTenantCommand;

/**
 * TenantCommandService - Service for Tenant CRUD operations
 */
public interface TenantCommandService {

    /**
     * Create a new Tenant
     */
    Tenant handle(CreateTenantCommand command);

    /**
     * Update an existing Tenant
     */
    Tenant handle(Integer tenantId, UpdateTenantCommand command);

    /**
     * Delete a Tenant (soft delete - mark as inactive)
     */
    void deleteTenant(Integer tenantId);
}
