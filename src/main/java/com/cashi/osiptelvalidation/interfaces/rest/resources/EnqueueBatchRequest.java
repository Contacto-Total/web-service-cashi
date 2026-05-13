package com.cashi.osiptelvalidation.interfaces.rest.resources;

import com.cashi.osiptelvalidation.domain.model.valueobjects.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request: POST /api/v1/osiptel/batches
 *
 * Post-pivot: encolar por DOCUMENTO (no por teléfono). Cada entrada produce
 * 1 fila en osiptel_validation_log; la respuesta del portal alimenta el
 * matching automático contra los metodos_contacto del cliente.
 *
 * El DNI plaintext NO se persiste en osiptel_validation_log.
 */
public record EnqueueBatchRequest(@Valid @NotEmpty List<DocumentItem> documents) {

    public record DocumentItem(
            @NotNull String dni,
            DocumentType dniType,        // null -> DNI por defecto
            @NotNull Long customerId,    // obligatorio para que el matching contra metodos_contacto funcione
            Long subPortfolioId,
            @NotNull Long tenantId
    ) {}
}
