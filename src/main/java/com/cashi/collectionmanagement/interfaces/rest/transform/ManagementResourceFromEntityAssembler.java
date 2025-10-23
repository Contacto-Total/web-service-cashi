package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.collectionmanagement.interfaces.rest.resources.ManagementResource;

public class ManagementResourceFromEntityAssembler {

    public static ManagementResource toResourceFromEntity(Management entity) {
        return new ManagementResource(
                entity.getId(),
                entity.getManagementId() != null ? entity.getManagementId().getManagementId() : null,
                entity.getCustomerId(),
                entity.getAdvisorId(),
                entity.getCampaignId(),
                entity.getManagementDate(),
                // Categoría y Tipificación
                entity.getCategoryCode(),
                entity.getCategoryDescription(),
                entity.getTypificationCode(),
                entity.getTypificationDescription(),
                entity.getTypificationRequiresPayment(),
                entity.getTypificationRequiresSchedule(),
                entity.getObservations(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
