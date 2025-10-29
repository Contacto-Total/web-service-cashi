package com.cashi.collectionmanagement.domain.model.commands;

public record UpdateManagementCommand(
    Long id,

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
