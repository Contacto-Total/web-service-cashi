package com.cashi.systemconfiguration.interfaces.rest.resources;

public record TenantResource(
    Integer id,
    String tenantCode,
    String tenantName,
    String businessName,
    Integer isActive,
    Boolean hasPortfolios
) {
}
