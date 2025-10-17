package com.cashi.customermanagement.interfaces.rest.resources;

/**
 * Resource (DTO) para respuesta de configuración de outputs del cliente
 *
 * EJEMPLO DE RESPUESTA:
 * {
 *   "id": 1,
 *   "tenantId": 2,
 *   "portfolioId": 5,
 *   "fieldsConfig": "[{\"id\":\"documentCode\",\"label\":\"DNI\",\"isVisible\":true,...}]"
 * }
 *
 * @param id ID de la configuración en BD
 * @param tenantId ID del tenant (financiera)
 * @param portfolioId ID del portfolio (cartera), null = configuración general
 * @param fieldsConfig JSON serializado con array de campos configurados
 */
public record CustomerOutputConfigResource(
        Long id,
        Long tenantId,
        Long portfolioId,
        String fieldsConfig
) {
}
