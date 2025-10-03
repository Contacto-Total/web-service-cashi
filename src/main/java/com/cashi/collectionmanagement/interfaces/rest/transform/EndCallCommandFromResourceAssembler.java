package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.commands.EndCallCommand;
import com.cashi.collectionmanagement.interfaces.rest.resources.EndCallRequest;

public class EndCallCommandFromResourceAssembler {

    public static EndCallCommand toCommandFromResource(String managementId, EndCallRequest resource) {
        return new EndCallCommand(
                managementId,
                resource.endTime()
        );
    }
}
