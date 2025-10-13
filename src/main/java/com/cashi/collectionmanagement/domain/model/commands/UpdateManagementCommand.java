package com.cashi.collectionmanagement.domain.model.commands;

public record UpdateManagementCommand(
    String managementId,

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
