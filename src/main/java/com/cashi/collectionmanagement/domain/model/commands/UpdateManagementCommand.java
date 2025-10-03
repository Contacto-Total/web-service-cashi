package com.cashi.collectionmanagement.domain.model.commands;

public record UpdateManagementCommand(
    String managementId,
    String contactResultCode,
    String contactResultDescription,
    String managementTypeCode,
    String managementTypeDescription,
    Boolean managementTypeRequiresPayment,
    Boolean managementTypeRequiresSchedule,
    String observations
) {
}
