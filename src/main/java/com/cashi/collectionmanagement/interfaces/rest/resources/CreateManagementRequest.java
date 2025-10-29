package com.cashi.collectionmanagement.interfaces.rest.resources;

/**
 * Request para crear una nueva gestión
 * @param customerId ID del cliente
 * @param advisorId ID del asesor
 * @param tenantId ID del inquilino (tenant)
 * @param portfolioId ID de la cartera
 * @param subPortfolioId ID de la subcartera (opcional)
 * @param phone Teléfono de contacto
 * @param level1Id ID de categoría nivel 1
 * @param level1Name Nombre de categoría nivel 1
 * @param level2Id ID de categoría nivel 2 (opcional)
 * @param level2Name Nombre de categoría nivel 2 (opcional)
 * @param level3Id ID de categoría nivel 3 (opcional)
 * @param level3Name Nombre de categoría nivel 3 (opcional)
 * @param observations Observaciones de la gestión
 */
public record CreateManagementRequest(
        String customerId,
        String advisorId,

        // Multi-tenant fields
        Integer tenantId,
        Integer portfolioId,
        Integer subPortfolioId,

        // Contact info
        String phone,

        // Hierarchical categorization (3 levels)
        Long level1Id,
        String level1Name,
        Long level2Id,
        String level2Name,
        Long level3Id,
        String level3Name,

        String observations
) {
}
