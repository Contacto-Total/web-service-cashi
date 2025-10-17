package com.cashi.customermanagement.domain.model.queries;

/**
 * Query para obtener configuración de outputs del cliente
 *
 * LÓGICA DE BÚSQUEDA:
 * 1. Si portfolioId != null → busca configuración específica del portfolio
 * 2. Si no encuentra específica → busca configuración general del tenant
 * 3. Si no encuentra ninguna → retorna null (frontend usa valores por defecto)
 *
 * @param tenantId ID del tenant (financiera)
 * @param portfolioId ID del portfolio (cartera), null = buscar solo general
 */
public record GetCustomerOutputConfigQuery(
        Long tenantId,
        Long portfolioId
) {
}
