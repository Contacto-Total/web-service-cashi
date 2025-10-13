package com.cashi.collectionmanagement.interfaces.rest.resources;

import java.time.LocalDateTime;

public record ManagementResource(
        Long id,
        String managementId,
        String customerId,
        String advisorId,
        String campaignId,
        LocalDateTime managementDate,

        // Clasificación: Categoría/grupo al que pertenece la tipificación
        String classificationCode,
        String classificationDescription,

        // Tipificación: Código específico/hoja (último nivel en jerarquía)
        String typificationCode,
        String typificationDescription,
        Boolean typificationRequiresPayment,
        Boolean typificationRequiresSchedule,

        CallDetailResource callDetail,
        PaymentDetailResource paymentDetail,
        String observations,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
