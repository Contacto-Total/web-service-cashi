package com.cashi.systemconfiguration.interfaces.rest.transform;

import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.systemconfiguration.interfaces.rest.resources.TenantResource;

public class TenantResourceFromEntityAssembler {

    public static TenantResource toResourceFromEntity(Tenant entity) {
        return toResourceFromEntity(entity, false);
    }

    public static TenantResource toResourceFromEntity(Tenant entity, boolean hasPortfolios) {
        return new TenantResource(
            entity.getId(),
            entity.getTenantCode(),
            entity.getTenantName(),
            entity.getBusinessName(),
            entity.getIsActive(),
            hasPortfolios
        );
    }
}
