package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.commands.CreateManagementCommand;
import com.cashi.collectionmanagement.interfaces.rest.resources.CreateManagementRequest;

public class CreateManagementCommandFromResourceAssembler {

    public static CreateManagementCommand toCommandFromResource(CreateManagementRequest resource) {
        return new CreateManagementCommand(
                resource.customerId(),
                resource.advisorId(),
                // Multi-tenant fields
                resource.tenantId(),
                resource.portfolioId(),
                resource.subPortfolioId(),
                resource.campaignId(),
                // Tipificaciones jerárquicas (3 niveles)
                resource.typificationLevel1Id(),
                resource.typificationLevel2Id(),
                resource.typificationLevel3Id(),
                resource.observations(),
                resource.dynamicFields()
        );
    }
}
