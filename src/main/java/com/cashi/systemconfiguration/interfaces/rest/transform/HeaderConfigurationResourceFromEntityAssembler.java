package com.cashi.systemconfiguration.interfaces.rest.transform;

import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.systemconfiguration.interfaces.rest.resources.HeaderConfigurationResource;

public class HeaderConfigurationResourceFromEntityAssembler {

    public static HeaderConfigurationResource toResourceFromEntity(HeaderConfiguration entity) {
        return new HeaderConfigurationResource(
            entity.getId(),
            entity.getSubPortfolio().getId(),
            entity.getFieldDefinition() != null ? entity.getFieldDefinition().getId() : 0,
            entity.getHeaderName(),
            entity.getDataType(),
            entity.getDisplayLabel(),
            entity.getFormat(),
            entity.getRequired() != null && entity.getRequired() == 1,
            entity.getLoadType(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
