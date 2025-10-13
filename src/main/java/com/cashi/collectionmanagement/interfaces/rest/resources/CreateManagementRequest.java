package com.cashi.collectionmanagement.interfaces.rest.resources;

import java.util.Map;

/**
 * Request para crear una nueva gestión
 * @param classificationCode Código de la clasificación/categoría (grupo)
 * @param classificationDescription Descripción de la clasificación
 * @param typificationCode Código de la tipificación específica (hoja)
 * @param typificationDescription Descripción de la tipificación
 * @param dynamicFields Campos dinámicos configurados por clasificación (JSON serializado)
 */
public record CreateManagementRequest(
        String customerId,
        String advisorId,
        String campaignId,

        // Clasificación: Categoría/grupo al que pertenece la tipificación
        String classificationCode,
        String classificationDescription,

        // Tipificación: Código específico/hoja (último nivel en jerarquía)
        String typificationCode,
        String typificationDescription,
        Boolean typificationRequiresPayment,
        Boolean typificationRequiresSchedule,

        String observations,
        Map<String, Object> dynamicFields
) {
}
