package com.cashi.osiptelvalidation.domain.model.commands;

import com.cashi.osiptelvalidation.domain.model.valueobjects.DocumentType;

import java.util.List;

/**
 * Comando: encolar un lote de DOCUMENTOS para validación Osiptel.
 *
 * Cada DocumentEntry produce 1 fila en osiptel_validation_log.
 * Idempotencia por UNIQUE(dni_hash, status PENDING/IN_PROGRESS).
 */
public record EnqueueOsiptelBatchCommand(List<DocumentEntry> entries) {

    /**
     * @param dni Número de documento plaintext. Se hashea internamente; NO se persiste.
     * @param dniType DNI | CE | PASAPORTE | RUC.
     * @param customerId FK opcional a clientes.id - permite matching automático de teléfonos.
     * @param subPortfolioId FK opcional a subcarteras.id - para reportería.
     * @param tenantId opcional - legado del hash con sal por tenant; hoy sal es global.
     */
    public record DocumentEntry(
            String dni,
            DocumentType dniType,
            Long customerId,
            Long subPortfolioId,
            Long tenantId
    ) {}
}
