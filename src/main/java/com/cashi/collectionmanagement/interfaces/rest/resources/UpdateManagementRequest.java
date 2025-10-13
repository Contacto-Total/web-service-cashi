package com.cashi.collectionmanagement.interfaces.rest.resources;

public record UpdateManagementRequest(
        // Clasificación: Categoría/grupo al que pertenece la tipificación
        String classificationCode,
        String classificationDescription,

        // Tipificación: Código específico/hoja (último nivel en jerarquía)
        String typificationCode,
        String typificationDescription,
        Boolean typificationRequiresPayment,
        Boolean typificationRequiresSchedule,

        String observations
) {
}
