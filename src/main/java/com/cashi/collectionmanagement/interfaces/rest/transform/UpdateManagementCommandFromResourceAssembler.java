package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.commands.UpdateManagementCommand;
import com.cashi.collectionmanagement.interfaces.rest.resources.UpdateManagementRequest;

public class UpdateManagementCommandFromResourceAssembler {

    public static UpdateManagementCommand toCommandFromResource(Long id, UpdateManagementRequest resource) {
        return new UpdateManagementCommand(
                id,
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
