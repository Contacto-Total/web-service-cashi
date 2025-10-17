package com.cashi.customermanagement.domain.services;

import com.cashi.customermanagement.domain.model.aggregates.CustomerOutputConfig;
import com.cashi.customermanagement.domain.model.commands.SaveCustomerOutputConfigCommand;

/**
 * Servicio de comandos para CustomerOutputConfig
 */
public interface CustomerOutputConfigCommandService {
    /**
     * Guarda o actualiza configuración de outputs
     *
     * LÓGICA:
     * - Si existe configuración para tenant+portfolio → actualiza
     * - Si no existe → crea nueva
     */
    CustomerOutputConfig handle(SaveCustomerOutputConfigCommand command);
}
