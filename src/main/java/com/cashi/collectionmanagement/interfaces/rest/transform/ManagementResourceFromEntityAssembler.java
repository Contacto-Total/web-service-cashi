package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.collectionmanagement.interfaces.rest.resources.ManagementResource;

public class ManagementResourceFromEntityAssembler {

    public static ManagementResource toResourceFromEntity(Management entity) {
        return new ManagementResource(
                entity.getId(),
                entity.getCustomerId(),
                entity.getAdvisorId(),

                // Multi-tenant fields
                entity.getTenant() != null ? entity.getTenant().getId() : (Integer) null,
                entity.getTenant() != null ? entity.getTenant().getTenantName() : null,
                entity.getPortfolio() != null ? entity.getPortfolio().getId() : (Integer) null,
                entity.getPortfolio() != null ? entity.getPortfolio().getPortfolioName() : null,
                entity.getSubPortfolio() != null ? entity.getSubPortfolio().getId() : (Integer) null,
                entity.getSubPortfolio() != null ? entity.getSubPortfolio().getSubPortfolioName() : null,

                // Contact info
                entity.getPhone(),

                // Hierarchical categorization
                entity.getLevel1Id(),
                entity.getLevel1Name(),
                entity.getLevel2Id(),
                entity.getLevel2Name(),
                entity.getLevel3Id(),
                entity.getLevel3Name(),

                entity.getObservations(),
                entity.getTypificationRequiresPayment(),
                entity.getTypificationRequiresSchedule(),

                // Automatic timestamp fields
                entity.getManagementDate() != null ? entity.getManagementDate().toString() : null,
                entity.getManagementTime() != null ? entity.getManagementTime().toString() : null
        );
    }
}
