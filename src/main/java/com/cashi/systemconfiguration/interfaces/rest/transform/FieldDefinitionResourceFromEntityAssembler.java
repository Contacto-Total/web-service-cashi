package com.cashi.systemconfiguration.interfaces.rest.transform;

import com.cashi.shared.domain.model.entities.FieldDefinition;
import com.cashi.systemconfiguration.interfaces.rest.resources.FieldDefinitionResource;

public class FieldDefinitionResourceFromEntityAssembler {

    public static FieldDefinitionResource toResourceFromEntity(FieldDefinition entity) {
        return new FieldDefinitionResource(
            entity.getId(),
            entity.getFieldCode(),
            entity.getFieldName(),
            entity.getDescription(),
            entity.getDataType(),
            entity.getFormat(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
