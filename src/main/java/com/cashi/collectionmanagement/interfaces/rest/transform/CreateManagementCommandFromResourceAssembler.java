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
                // Contact info
                resource.phone(),
                // Hierarchical categorization (3 levels)
                resource.level1Id(),
                resource.level1Name(),
                resource.level2Id(),
                resource.level2Name(),
                resource.level3Id(),
                resource.level3Name(),
                resource.observations()
        );
    }
}
