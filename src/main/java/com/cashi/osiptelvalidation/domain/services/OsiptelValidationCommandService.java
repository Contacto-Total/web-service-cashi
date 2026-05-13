package com.cashi.osiptelvalidation.domain.services;

import com.cashi.osiptelvalidation.domain.model.commands.EnqueueOsiptelBatchCommand;
import com.cashi.osiptelvalidation.domain.model.commands.RecordValidationResultCommand;
import com.cashi.osiptelvalidation.domain.model.valueobjects.DocumentType;

import java.util.List;

/**
 * Contrato de comandos del bounded context osiptelvalidation.
 * Implementación en application/internal/commandservices/OsiptelValidationCommandServiceImpl.
 */
public interface OsiptelValidationCommandService {

    /** Encola un lote para validación. */
    EnqueueResult enqueueBatch(EnqueueOsiptelBatchCommand command);

    /**
     * Reclama hasta `limit` jobs PENDING y los marca IN_PROGRESS.
     * Resuelve el DNI plaintext desde clientes.documento via source_customer_id.
     * El Electron app polea este endpoint.
     */
    List<ClaimedJob> claimJobs(String workerId, int limit);

    /** Persiste el resultado entregado por el Electron app tras procesar un job. */
    void recordResult(RecordValidationResultCommand command);

    /**
     * Reclama filas en IN_PROGRESS con started_at más viejo que el umbral.
     * Las devuelve a PENDING para reintentar.
     */
    int reclaimStuckInProgress(int olderThanMinutes);

    record EnqueueResult(int enqueued, int skipped, String batchId) {}

    /** Job listo para enviar al worker remoto: incluye DNI plaintext. */
    record ClaimedJob(Long validationId, String dni, DocumentType dniType) {}
}
