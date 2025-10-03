package com.cashi.systemconfiguration.interfaces.rest.transform;

import com.cashi.systemconfiguration.domain.model.entities.ContactClassification;
import com.cashi.systemconfiguration.interfaces.rest.resources.ContactClassificationResource;

public class ContactClassificationResourceFromEntityAssembler {

    public static ContactClassificationResource toResourceFromEntity(ContactClassification entity) {
        return new ContactClassificationResource(
                entity.getId(),
                entity.getCode(),
                entity.getLabel(),
                entity.getIsSuccessful()
        );
    }
}
