package com.cashi.collectionmanagement.domain.model.commands;

import java.util.Map;

public record CreateManagementCommand(
    String customerId,
    String advisorId,
    String campaignId,

    // Categoría: Grupo al que pertenece la tipificación
    String categoryCode,
    String categoryDescription,

    // Tipificación: Código específico/hoja (último nivel en jerarquía)
    String typificationCode,
    String typificationDescription,
    Boolean typificationRequiresPayment,
    Boolean typificationRequiresSchedule,

    String observations,
    Map<String, Object> dynamicFields
) {
}
