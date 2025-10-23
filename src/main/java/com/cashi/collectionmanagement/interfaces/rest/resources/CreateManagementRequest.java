package com.cashi.collectionmanagement.interfaces.rest.resources;

import java.util.Map;

/**
 * Request para crear una nueva gestión
 * @param categoryCode Código de la categoría (grupo)
 * @param categoryDescription Descripción de la categoría
 * @param typificationCode Código de la tipificación específica (hoja)
 * @param typificationDescription Descripción de la tipificación
 * @param dynamicFields Campos dinámicos configurados por tipificación (JSON serializado)
 */
public record CreateManagementRequest(
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
