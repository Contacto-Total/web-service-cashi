package com.cashi.osiptelvalidation.domain.model.commands;

import java.util.List;

/**
 * Comando: encolar un lote de números para validación Osiptel.
 *
 * Cada PhoneEntry es procesado independientemente. Idempotencia por
 * UNIQUE(phone, status PENDING/IN_PROGRESS) de la tabla.
 */
public record EnqueueOsiptelBatchCommand(List<PhoneEntry> entries) {

    /**
     * @param phone número crudo (se valida y normaliza al construir).
     * @param dni DNI plaintext del cliente. NO se persiste; se hashea con sal de tenant.
     * @param subPortfolioId origen del candidato (opcional, para reportería).
     * @param contactMethodId FK opcional a metodos_contacto.id.
     * @param tenantId requerido para derivar la sal del hash de DNI.
     */
    public record PhoneEntry(
            String phone,
            String dni,
            Long subPortfolioId,
            Long contactMethodId,
            Long tenantId
    ) {}
}
