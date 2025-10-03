package com.cashi.systemconfiguration.interfaces.rest.resources;

public record ManagementClassificationResource(
        Long id,
        String code,
        String label,
        Boolean requiresPayment,
        Boolean requiresSchedule,
        Boolean requiresFollowUp
) {
}
