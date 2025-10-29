package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.collectionmanagement.interfaces.rest.resources.ManagementResource;

public class ManagementResourceFromEntityAssembler {

    public static ManagementResource toResourceFromEntity(Management entity) {
        return new ManagementResource(
                entity.getId(),  // ID from AggregateRoot (Long auto-increment)
                entity.getId() != null ? entity.getId().toString() : null,  // managementId as string version of id
                entity.getCustomerId(),
                entity.getAdvisorId(),
                entity.getCampaign() != null ? entity.getCampaign().getId().toString() : null,  // Obtener ID de Campaign entity
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
