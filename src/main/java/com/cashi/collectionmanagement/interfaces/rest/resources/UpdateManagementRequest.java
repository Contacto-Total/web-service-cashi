package com.cashi.collectionmanagement.interfaces.rest.resources;

public record UpdateManagementRequest(
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
