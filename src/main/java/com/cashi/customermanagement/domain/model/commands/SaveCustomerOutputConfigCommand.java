package com.cashi.customermanagement.domain.model.commands;

/**
 * Comando para guardar/actualizar configuración de outputs del cliente
 *
 * FLUJO:
 * 1. Frontend envía: POST /api/v1/customer-outputs/config
 * 2. Controller convierte request → command
 * 3. CommandService procesa comando
 * 4. Si existe config para tenant+portfolio → actualiza
 * 5. Si no existe → crea nueva
 *
 * @param tenantId ID del tenant (financiera)
 * @param portfolioId ID del portfolio (cartera), null = configuración general
 * @param fieldsConfig JSON con configuración de campos
 */
public record SaveCustomerOutputConfigCommand(
        Long tenantId,
        Long portfolioId,
        String fieldsConfig
) {
}
