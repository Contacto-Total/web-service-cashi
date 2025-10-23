package com.cashi.collectionmanagement.interfaces.rest.resources;

import java.time.LocalDateTime;

public record ManagementResource(
        Long id,
        String managementId,
        String customerId,
        String advisorId,
        String campaignId,
        LocalDateTime managementDate,

        // Categoría: Grupo al que pertenece la tipificación
        String categoryCode,
        String categoryDescription,

        // Tipificación: Código específico/hoja (último nivel en jerarquía)
        String typificationCode,
        String typificationDescription,
        Boolean typificationRequiresPayment,
        Boolean typificationRequiresSchedule,

        String observations,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
