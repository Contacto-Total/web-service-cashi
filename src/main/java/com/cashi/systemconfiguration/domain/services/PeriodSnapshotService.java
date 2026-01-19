package com.cashi.systemconfiguration.domain.services;

import java.time.YearMonth;
import java.util.Optional;

/**
 * Servicio para gestionar snapshots de periodo y cambios de mes.
 * Maneja el archivado histórico de tablas dinámicas cuando se realiza
 * una nueva carga inicial de mes.
 */
public interface PeriodSnapshotService {

    /**
     * Información del periodo actual de una subcartera
     */
    record PeriodInfo(
        Long subPortfolioId,
        String tableName,
        boolean hasExistingData,
        long recordCount,
        YearMonth currentPeriod,
        String lastLoadDate
    ) {}

    /**
     * Resultado de la ejecución del snapshot
     */
    record SnapshotResult(
        boolean success,
        int tablesArchived,
        String archivePeriod,
        String message,
        long executionTimeMs
    ) {}

    /**
     * Verifica el estado del periodo actual para una subcartera.
     * Usado para determinar si una nueva carga inicial representa un cambio de periodo.
     *
     * @param subPortfolioId ID de la subcartera
     * @return Información del periodo actual
     */
    PeriodInfo checkPeriodStatus(Long subPortfolioId);

    /**
     * Ejecuta el snapshot de todas las carteras para archivar el periodo anterior.
     * Llama al stored procedure sp_snapshot_carteras_mensual.
     *
     * @return Resultado de la operación de snapshot
     */
    SnapshotResult executeGlobalSnapshot();

    /**
     * Ejecuta snapshot solo para una subcartera específica.
     *
     * @param subPortfolioId ID de la subcartera
     * @return Resultado de la operación
     */
    SnapshotResult executeSnapshotForSubPortfolio(Long subPortfolioId);

    /**
     * Verifica si se requiere confirmación de cambio de periodo.
     * Retorna true si ya hay datos cargados en la tabla inicial.
     *
     * @param subPortfolioId ID de la subcartera
     * @return true si hay datos existentes que serían afectados
     */
    boolean requiresPeriodChangeConfirmation(Long subPortfolioId);

    /**
     * Obtiene el periodo anterior archivado más reciente para una subcartera.
     *
     * @param subPortfolioId ID de la subcartera
     * @return Periodo archivado más reciente (ej: "2025_12") o empty si no hay
     */
    Optional<String> getLastArchivedPeriod(Long subPortfolioId);

    // ==================== Métodos para Snapshot Diario ====================

    /**
     * Información del estado diario de una subcartera (tabla de actualización)
     */
    record DailyInfo(
        Long subPortfolioId,
        String tableName,
        boolean hasExistingData,
        long recordCount,
        String lastLoadDate,
        String lastArchivedDate
    ) {}

    /**
     * Verifica el estado de la tabla de actualización (carga diaria) para una subcartera.
     * Usado para determinar si una nueva carga diaria requiere archivar datos existentes.
     *
     * @param subPortfolioId ID de la subcartera
     * @return Información del estado de la tabla de actualización
     */
    DailyInfo checkDailyStatus(Long subPortfolioId);

    /**
     * Verifica si se requiere confirmación para la carga diaria.
     * Retorna true si ya hay datos cargados en la tabla de actualización.
     *
     * @param subPortfolioId ID de la subcartera
     * @return true si hay datos existentes que serían afectados
     */
    boolean requiresDailyChangeConfirmation(Long subPortfolioId);

    /**
     * Ejecuta snapshot diario para una subcartera específica.
     * Archiva la tabla de actualización con fecha del día.
     *
     * @param subPortfolioId ID de la subcartera
     * @return Resultado de la operación
     */
    SnapshotResult executeDailySnapshotForSubPortfolio(Long subPortfolioId);

    /**
     * Obtiene la última fecha de archivo diario para una subcartera.
     *
     * @param subPortfolioId ID de la subcartera
     * @return Fecha archivada más reciente (ej: "2025_01_19") o empty si no hay
     */
    Optional<String> getLastArchivedDailyDate(Long subPortfolioId);
}
