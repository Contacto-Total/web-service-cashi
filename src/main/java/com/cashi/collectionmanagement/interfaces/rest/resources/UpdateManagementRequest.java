package com.cashi.collectionmanagement.interfaces.rest.resources;

public record UpdateManagementRequest(
        // Categoría: Grupo al que pertenece la tipificación
        String categoryCode,
        String categoryDescription,

        // Tipificación: Código específico/hoja (último nivel en jerarquía)
        String typificationCode,
        String typificationDescription,
        Boolean typificationRequiresPayment,
        Boolean typificationRequiresSchedule,

        String observations
) {
}
