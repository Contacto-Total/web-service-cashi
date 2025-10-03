package com.cashi.systemconfiguration.interfaces.rest.resources;

public record TenantResource(
    Long id,
    String tenantCode,
    String tenantName,
    String businessName,
    Boolean isActive
) {
}
