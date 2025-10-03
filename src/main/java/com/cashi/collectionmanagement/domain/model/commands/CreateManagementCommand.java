package com.cashi.collectionmanagement.domain.model.commands;

public record CreateManagementCommand(
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
