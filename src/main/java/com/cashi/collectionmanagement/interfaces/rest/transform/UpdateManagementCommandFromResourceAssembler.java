package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.commands.UpdateManagementCommand;
import com.cashi.collectionmanagement.interfaces.rest.resources.UpdateManagementRequest;

public class UpdateManagementCommandFromResourceAssembler {

    public static UpdateManagementCommand toCommandFromResource(String managementId, UpdateManagementRequest resource) {
        return new UpdateManagementCommand(
                managementId,
                resource.contactResultCode(),
                resource.contactResultDescription(),
                resource.managementTypeCode(),
                resource.managementTypeDescription(),
                resource.managementTypeRequiresPayment(),
                resource.managementTypeRequiresSchedule(),
                resource.observations()
        );
    }
}
