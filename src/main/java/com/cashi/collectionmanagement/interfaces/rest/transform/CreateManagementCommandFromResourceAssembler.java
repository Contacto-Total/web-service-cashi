package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.commands.CreateManagementCommand;
import com.cashi.collectionmanagement.interfaces.rest.resources.CreateManagementRequest;

public class CreateManagementCommandFromResourceAssembler {

    public static CreateManagementCommand toCommandFromResource(CreateManagementRequest resource) {
        return new CreateManagementCommand(
                resource.customerId(),
                resource.advisorId(),
                resource.campaignId(),
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
