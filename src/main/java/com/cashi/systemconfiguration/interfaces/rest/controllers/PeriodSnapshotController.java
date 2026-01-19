package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.systemconfiguration.domain.services.PeriodSnapshotService;
import com.cashi.systemconfiguration.domain.services.PeriodSnapshotService.PeriodInfo;
import com.cashi.systemconfiguration.domain.services.PeriodSnapshotService.SnapshotResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/system-config/period-snapshot")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Period Snapshot", description = "Gestión de snapshots y cambios de periodo mensual")
public class PeriodSnapshotController {

    private final PeriodSnapshotService periodSnapshotService;

    public PeriodSnapshotController(PeriodSnapshotService periodSnapshotService) {
        this.periodSnapshotService = periodSnapshotService;
    }

    @Operation(summary = "Verificar estado del periodo para una subcartera",
            description = "Retorna información sobre el periodo actual y si hay datos existentes que serían afectados por un cambio de periodo")
    @GetMapping("/subportfolio/{subPortfolioId}/status")
    public ResponseEntity<PeriodStatusResponse> checkPeriodStatus(@PathVariable Long subPortfolioId) {
        PeriodInfo info = periodSnapshotService.checkPeriodStatus(subPortfolioId);
        Optional<String> lastArchived = periodSnapshotService.getLastArchivedPeriod(subPortfolioId);

        PeriodStatusResponse response = new PeriodStatusResponse(
            info.subPortfolioId(),
            info.tableName(),
            info.hasExistingData(),
            info.recordCount(),
            info.currentPeriod() != null ? info.currentPeriod().toString() : null,
            info.lastLoadDate(),
            lastArchived.orElse(null),
            info.hasExistingData() // requiresConfirmation
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verificar si se requiere confirmación de cambio de periodo",
            description = "Retorna true si ya existen datos en la tabla inicial que serían archivados")
    @GetMapping("/subportfolio/{subPortfolioId}/requires-confirmation")
    public ResponseEntity<Map<String, Object>> requiresConfirmation(@PathVariable Long subPortfolioId) {
        boolean requires = periodSnapshotService.requiresPeriodChangeConfirmation(subPortfolioId);
        PeriodInfo info = periodSnapshotService.checkPeriodStatus(subPortfolioId);

        return ResponseEntity.ok(Map.of(
            "requiresConfirmation", requires,
            "recordCount", info.recordCount(),
            "currentPeriod", info.currentPeriod() != null ? info.currentPeriod().toString() : "",
            "tableName", info.tableName()
        ));
    }

    @Operation(summary = "Ejecutar snapshot para una subcartera específica",
            description = "Archiva las tablas de la subcartera al periodo actual antes de una nueva carga inicial")
    @PostMapping("/subportfolio/{subPortfolioId}/execute")
    public ResponseEntity<SnapshotResultResponse> executeSnapshotForSubPortfolio(@PathVariable Long subPortfolioId) {
        SnapshotResult result = periodSnapshotService.executeSnapshotForSubPortfolio(subPortfolioId);

        return ResponseEntity.ok(new SnapshotResultResponse(
            result.success(),
            result.tablesArchived(),
            result.archivePeriod(),
            result.message(),
            result.executionTimeMs()
        ));
    }

    @Operation(summary = "Ejecutar snapshot global de todas las carteras",
            description = "Ejecuta el stored procedure sp_snapshot_carteras_mensual para archivar todas las carteras")
    @PostMapping("/execute-global")
    public ResponseEntity<SnapshotResultResponse> executeGlobalSnapshot() {
        SnapshotResult result = periodSnapshotService.executeGlobalSnapshot();

        return ResponseEntity.ok(new SnapshotResultResponse(
            result.success(),
            result.tablesArchived(),
            result.archivePeriod(),
            result.message(),
            result.executionTimeMs()
        ));
    }

    @Operation(summary = "Obtener último periodo archivado para una subcartera")
    @GetMapping("/subportfolio/{subPortfolioId}/last-archived")
    public ResponseEntity<Map<String, Object>> getLastArchivedPeriod(@PathVariable Long subPortfolioId) {
        Optional<String> lastArchived = periodSnapshotService.getLastArchivedPeriod(subPortfolioId);

        return ResponseEntity.ok(Map.of(
            "subPortfolioId", subPortfolioId,
            "lastArchivedPeriod", lastArchived.orElse(null),
            "hasArchivedData", lastArchived.isPresent()
        ));
    }

    // ==================== Response DTOs ====================

    public record PeriodStatusResponse(
        Long subPortfolioId,
        String tableName,
        boolean hasExistingData,
        long recordCount,
        String currentPeriod,
        String lastLoadDate,
        String lastArchivedPeriod,
        boolean requiresConfirmation
    ) {}

    public record SnapshotResultResponse(
        boolean success,
        int tablesArchived,
        String archivePeriod,
        String message,
        long executionTimeMs
    ) {}
}
