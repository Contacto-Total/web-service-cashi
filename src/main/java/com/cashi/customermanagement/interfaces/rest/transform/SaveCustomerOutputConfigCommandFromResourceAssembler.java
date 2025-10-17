package com.cashi.customermanagement.interfaces.rest.transform;

import com.cashi.customermanagement.domain.model.commands.SaveCustomerOutputConfigCommand;
import com.cashi.customermanagement.interfaces.rest.resources.SaveCustomerOutputConfigRequest;

/**
 * Assembler para convertir Request → Command
 *
 * FLUJO:
 * 1. Controller recibe SaveCustomerOutputConfigRequest (JSON)
 * 2. Assembler convierte Request → SaveCustomerOutputConfigCommand
 * 3. Command se pasa al CommandService
 */
public class SaveCustomerOutputConfigCommandFromResourceAssembler {

    /**
     * Convierte Request a Command
     */
    public static SaveCustomerOutputConfigCommand toCommandFromResource(SaveCustomerOutputConfigRequest request) {
        return new SaveCustomerOutputConfigCommand(
            request.tenantId(),
            request.portfolioId(),
            request.fieldsConfig()
        );
    }
}
