package com.cashi.osiptelvalidation.domain.services;

import com.cashi.osiptelvalidation.domain.model.commands.EnqueueOsiptelBatchCommand;
import com.cashi.osiptelvalidation.domain.model.commands.RecordValidationResultCommand;

/**
 * Contrato de comandos del bounded context osiptelvalidation.
 * Implementación en application/internal/commandservices/OsiptelValidationCommandServiceImpl.
 */
public interface OsiptelValidationCommandService {

    /**
     * Encola un lote para validación.
     * @return resultado del encolado con conteos de aceptados/rechazados.
     */
    EnqueueResult enqueueBatch(EnqueueOsiptelBatchCommand command);

    /**
     * Persiste el resultado de un check finalizado por el worker.
     * Aplica la transición correspondiente al aggregate.
     */
    void recordResult(RecordValidationResultCommand command);

    /**
     * Reclama filas en IN_PROGRESS con started_at más viejo que el umbral.
     * Las devuelve a PENDING para reintentar.
     * Llamado por el dispatcher como housekeeping.
     */
    int reclaimStuckInProgress(int olderThanMinutes);

    record EnqueueResult(int enqueued, int skipped, String batchId) {}
}
