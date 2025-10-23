package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.commands.UpdateManagementCommand;
import com.cashi.collectionmanagement.interfaces.rest.resources.UpdateManagementRequest;

public class UpdateManagementCommandFromResourceAssembler {

    public static UpdateManagementCommand toCommandFromResource(String managementId, UpdateManagementRequest resource) {
        return new UpdateManagementCommand(
                managementId,
                resource.categoryCode(),
                resource.categoryDescription(),
                resource.typificationCode(),
                resource.typificationDescription(),
                resource.typificationRequiresPayment(),
                resource.typificationRequiresSchedule(),
                resource.observations()
        );
    }
}
