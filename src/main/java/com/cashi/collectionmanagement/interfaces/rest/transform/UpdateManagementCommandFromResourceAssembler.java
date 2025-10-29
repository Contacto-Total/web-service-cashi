package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.commands.UpdateManagementCommand;
import com.cashi.collectionmanagement.interfaces.rest.resources.UpdateManagementRequest;

public class UpdateManagementCommandFromResourceAssembler {

    public static UpdateManagementCommand toCommandFromResource(Long id, UpdateManagementRequest resource) {
        return new UpdateManagementCommand(
                id,
                resource.phone(),
                resource.level1Id(),
                resource.level1Name(),
                resource.level2Id(),
                resource.level2Name(),
                resource.level3Id(),
                resource.level3Name(),
                resource.observations(),
                resource.typificationRequiresPayment(),
                resource.typificationRequiresSchedule()
        );
    }
}
