package com.cashi.collectionmanagement.interfaces.rest.resources;

import java.time.LocalDateTime;

public record ManagementResource(
        Long id,
        String managementId,
        String customerId,
        String advisorId,
        String campaignId,
        LocalDateTime managementDate,
        String contactResultCode,
        String contactResultDescription,
        String managementTypeCode,
        String managementTypeDescription,
        Boolean managementTypeRequiresPayment,
        Boolean managementTypeRequiresSchedule,
        CallDetailResource callDetail,
        PaymentDetailResource paymentDetail,
        String observations,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
