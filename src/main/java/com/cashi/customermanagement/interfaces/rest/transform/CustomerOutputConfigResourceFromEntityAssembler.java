package com.cashi.customermanagement.interfaces.rest.transform;

import com.cashi.customermanagement.domain.model.aggregates.CustomerOutputConfig;
import com.cashi.customermanagement.interfaces.rest.resources.CustomerOutputConfigResource;

/**
 * Assembler para convertir Entity → Resource
 *
 * FLUJO:
 * 1. QueryService retorna CustomerOutputConfig (entidad)
 * 2. Assembler convierte Entity → CustomerOutputConfigResource (DTO)
 * 3. Resource se serializa a JSON y se envía al frontend
 */
public class CustomerOutputConfigResourceFromEntityAssembler {

    /**
     * Convierte Entity a Resource
     */
    public static CustomerOutputConfigResource toResourceFromEntity(CustomerOutputConfig entity) {
        return new CustomerOutputConfigResource(
            entity.getId(),
            entity.getTenantId(),
            entity.getPortfolioId(),
            entity.getFieldsConfig()
        );
    }
}
