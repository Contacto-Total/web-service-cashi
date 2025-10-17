package com.cashi.customermanagement.interfaces.rest.resources;

/**
 * Request para guardar configuración de outputs del cliente
 *
 * EJEMPLO DE USO (desde frontend):
 * POST /api/v1/customer-outputs/config
 * {
 *   "tenantId": 2,
 *   "portfolioId": 5,  // null para configuración general
 *   "fieldsConfig": "[{\"id\":\"documentCode\",\"label\":\"DNI\",\"isVisible\":true,...}]"
 * }
 *
 * @param tenantId ID del tenant (financiera)
 * @param portfolioId ID del portfolio (cartera), null = configuración general
 * @param fieldsConfig JSON serializado con array de campos configurados
 */
public record SaveCustomerOutputConfigRequest(
        Long tenantId,
        Long portfolioId,
        String fieldsConfig
) {
}
