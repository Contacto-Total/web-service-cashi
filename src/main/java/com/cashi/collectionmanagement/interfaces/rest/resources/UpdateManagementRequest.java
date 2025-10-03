package com.cashi.collectionmanagement.interfaces.rest.resources;

public record UpdateManagementRequest(
        String contactResultCode,
        String contactResultDescription,
        String managementTypeCode,
        String managementTypeDescription,
        Boolean managementTypeRequiresPayment,
        Boolean managementTypeRequiresSchedule,
        String observations
) {
}
