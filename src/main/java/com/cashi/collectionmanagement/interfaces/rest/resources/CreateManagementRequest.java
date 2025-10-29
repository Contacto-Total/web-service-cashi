package com.cashi.collectionmanagement.interfaces.rest.resources;

import java.util.Map;

/**
 * Request para crear una nueva gestión
 * @param tenantId ID del inquilino (tenant)
 * @param portfolioId ID de la cartera
 * @param subPortfolioId ID de la subcartera (opcional)
 * @param campaignId ID de la campaña (como entidad, no legacy)
 * @param typificationLevel1Id ID de la tipificación nivel 1
 * @param typificationLevel2Id ID de la tipificación nivel 2
 * @param typificationLevel3Id ID de la tipificación nivel 3
 * @param dynamicFields Campos dinámicos configurados por tipificación (JSON serializado)
 */
public record CreateManagementRequest(
        String customerId,
        String advisorId,

        // Multi-tenant fields
        Integer tenantId,
        Integer portfolioId,
        Integer subPortfolioId,
        Long campaignId,  // Campaign usa Long como ID

        // Jerarquía de tipificaciones (3 niveles)
        Integer typificationLevel1Id,
        Integer typificationLevel2Id,
        Integer typificationLevel3Id,

        String observations,
        Map<String, Object> dynamicFields
) {
}
