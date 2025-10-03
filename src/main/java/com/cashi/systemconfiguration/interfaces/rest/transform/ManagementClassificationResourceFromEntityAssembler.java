package com.cashi.systemconfiguration.interfaces.rest.transform;

import com.cashi.systemconfiguration.domain.model.entities.ManagementClassification;
import com.cashi.systemconfiguration.interfaces.rest.resources.ManagementClassificationResource;

public class ManagementClassificationResourceFromEntityAssembler {

    public static ManagementClassificationResource toResourceFromEntity(ManagementClassification entity) {
        return new ManagementClassificationResource(
                entity.getId(),
                entity.getCode(),
                entity.getLabel(),
                entity.getRequiresPayment(),
                entity.getRequiresSchedule(),
                entity.getRequiresFollowUp()
        );
    }
}
