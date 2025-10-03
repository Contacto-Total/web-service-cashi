package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.commands.StartCallCommand;
import com.cashi.collectionmanagement.interfaces.rest.resources.StartCallRequest;

public class StartCallCommandFromResourceAssembler {

    public static StartCallCommand toCommandFromResource(String managementId, StartCallRequest resource) {
        return new StartCallCommand(
                managementId,
                resource.phoneNumber(),
                resource.startTime()
        );
    }
}
