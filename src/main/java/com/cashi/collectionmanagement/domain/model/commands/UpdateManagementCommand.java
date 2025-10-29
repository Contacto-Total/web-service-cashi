package com.cashi.collectionmanagement.domain.model.commands;

public record UpdateManagementCommand(
    Long id,
    String phone,
    Long level1Id,
    String level1Name,
    Long level2Id,
    String level2Name,
    Long level3Id,
    String level3Name,
    String observations,
    Boolean typificationRequiresPayment,
    Boolean typificationRequiresSchedule
) {
}
