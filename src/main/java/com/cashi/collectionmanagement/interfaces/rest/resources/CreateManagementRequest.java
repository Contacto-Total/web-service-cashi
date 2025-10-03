package com.cashi.collectionmanagement.interfaces.rest.resources;

public record CreateManagementRequest(
        String customerId,
        String advisorId,
        String campaignId,
        String contactResultCode,
        String contactResultDescription,
        String managementTypeCode,
        String managementTypeDescription,
        Boolean managementTypeRequiresPayment,
        Boolean managementTypeRequiresSchedule,
        String observations
) {
}
