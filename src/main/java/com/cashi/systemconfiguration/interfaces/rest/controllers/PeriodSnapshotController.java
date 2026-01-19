package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.systemconfiguration.domain.services.PeriodSnapshotService;
import com.cashi.systemconfiguration.domain.services.PeriodSnapshotService.DailyInfo;
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
    public ResponseEntity<?> checkPeriodStatus(@PathVariable Integer subPortfolioId) {
        try {
            System.out.println("📊 [PeriodSnapshot] Verificando estado para subPortfolioId: " + subPortfolioId);

            PeriodInfo info = periodSnapshotService.checkPeriodStatus(subPortfolioId.longValue());
            System.out.println("📊 [PeriodSnapshot] PeriodInfo obtenido: " + info);

            Optional<String> lastArchived = periodSnapshotService.getLastArchivedPeriod(subPortfolioId.longValue());
            System.out.println("📊 [PeriodSnapshot] LastArchived: " + lastArchived.orElse("ninguno"));

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

            System.out.println("✅ [PeriodSnapshot] Respuesta: requiresConfirmation=" + response.requiresConfirmation()
                + ", recordCount=" + response.recordCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ [PeriodSnapshot] Error en checkPeriodStatus: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Verificar si se requiere confirmación de cambio de periodo",
            description = "Retorna true si ya existen datos en la tabla inicial que serían archivados")
    @GetMapping("/subportfolio/{subPortfolioId}/requires-confirmation")
    public ResponseEntity<?> requiresConfirmation(@PathVariable Integer subPortfolioId) {
        try {
            boolean requires = periodSnapshotService.requiresPeriodChangeConfirmation(subPortfolioId.longValue());
            PeriodInfo info = periodSnapshotService.checkPeriodStatus(subPortfolioId.longValue());

            return ResponseEntity.ok(Map.of(
                "requiresConfirmation", requires,
                "recordCount", info.recordCount(),
                "currentPeriod", info.currentPeriod() != null ? info.currentPeriod().toString() : "",
                "tableName", info.tableName()
            ));
        } catch (Exception e) {
            System.err.println("❌ [PeriodSnapshot] Error en requiresConfirmation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Ejecutar snapshot para una subcartera específica",
            description = "Archiva las tablas de la subcartera al periodo actual antes de una nueva carga inicial")
    @PostMapping("/subportfolio/{subPortfolioId}/execute")
    public ResponseEntity<?> executeSnapshotForSubPortfolio(@PathVariable Integer subPortfolioId) {
        try {
            System.out.println("🗄️ [PeriodSnapshot] Ejecutando snapshot para subPortfolioId: " + subPortfolioId);

            SnapshotResult result = periodSnapshotService.executeSnapshotForSubPortfolio(subPortfolioId.longValue());

            System.out.println("✅ [PeriodSnapshot] Snapshot completado: success=" + result.success()
                + ", tablesArchived=" + result.tablesArchived());

            return ResponseEntity.ok(new SnapshotResultResponse(
                result.success(),
                result.tablesArchived(),
                result.archivePeriod(),
                result.message(),
                result.executionTimeMs()
            ));
        } catch (Exception e) {
            System.err.println("❌ [PeriodSnapshot] Error en executeSnapshotForSubPortfolio: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
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
    public ResponseEntity<?> getLastArchivedPeriod(@PathVariable Integer subPortfolioId) {
        try {
            Optional<String> lastArchived = periodSnapshotService.getLastArchivedPeriod(subPortfolioId.longValue());

            return ResponseEntity.ok(Map.of(
                "subPortfolioId", subPortfolioId,
                "lastArchivedPeriod", lastArchived.orElse(null),
                "hasArchivedData", lastArchived.isPresent()
            ));
        } catch (Exception e) {
            System.err.println("❌ [PeriodSnapshot] Error en getLastArchivedPeriod: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Endpoints para Snapshot Diario ====================

    @Operation(summary = "Verificar estado diario para una subcartera",
            description = "Retorna información sobre la tabla de actualización y si hay datos que serían archivados")
    @GetMapping("/subportfolio/{subPortfolioId}/daily-status")
    public ResponseEntity<?> checkDailyStatus(@PathVariable Integer subPortfolioId) {
        try {
            System.out.println("📊 [DailySnapshot] Verificando estado diario para subPortfolioId: " + subPortfolioId);

            DailyInfo info = periodSnapshotService.checkDailyStatus(subPortfolioId.longValue());
            System.out.println("📊 [DailySnapshot] DailyInfo obtenido: " + info);

            DailyStatusResponse response = new DailyStatusResponse(
                info.subPortfolioId(),
                info.tableName(),
                info.hasExistingData(),
                info.recordCount(),
                info.lastLoadDate(),
                info.lastArchivedDate(),
                info.hasExistingData() // requiresConfirmation
            );

            System.out.println("✅ [DailySnapshot] Respuesta: requiresConfirmation=" + response.requiresConfirmation()
                + ", recordCount=" + response.recordCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ [DailySnapshot] Error en checkDailyStatus: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Verificar si se requiere confirmación para carga diaria",
            description = "Retorna true si ya existen datos en la tabla de actualización que serían archivados")
    @GetMapping("/subportfolio/{subPortfolioId}/daily-requires-confirmation")
    public ResponseEntity<?> requiresDailyConfirmation(@PathVariable Integer subPortfolioId) {
        try {
            boolean requires = periodSnapshotService.requiresDailyChangeConfirmation(subPortfolioId.longValue());
            DailyInfo info = periodSnapshotService.checkDailyStatus(subPortfolioId.longValue());

            return ResponseEntity.ok(Map.of(
                "requiresConfirmation", requires,
                "recordCount", info.recordCount(),
                "lastLoadDate", info.lastLoadDate() != null ? info.lastLoadDate() : "",
                "tableName", info.tableName()
            ));
        } catch (Exception e) {
            System.err.println("❌ [DailySnapshot] Error en requiresDailyConfirmation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Ejecutar snapshot diario para una subcartera",
            description = "Archiva la tabla de actualización antes de una nueva carga diaria")
    @PostMapping("/subportfolio/{subPortfolioId}/daily-execute")
    public ResponseEntity<?> executeDailySnapshotForSubPortfolio(@PathVariable Integer subPortfolioId) {
        try {
            System.out.println("🗄️ [DailySnapshot] Ejecutando snapshot diario para subPortfolioId: " + subPortfolioId);

            SnapshotResult result = periodSnapshotService.executeDailySnapshotForSubPortfolio(subPortfolioId.longValue());

            System.out.println("✅ [DailySnapshot] Snapshot diario completado: success=" + result.success()
                + ", tablesArchived=" + result.tablesArchived());

            return ResponseEntity.ok(new SnapshotResultResponse(
                result.success(),
                result.tablesArchived(),
                result.archivePeriod(),
                result.message(),
                result.executionTimeMs()
            ));
        } catch (Exception e) {
            System.err.println("❌ [DailySnapshot] Error en executeDailySnapshotForSubPortfolio: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Obtener última fecha de archivo diario para una subcartera")
    @GetMapping("/subportfolio/{subPortfolioId}/last-daily-archived")
    public ResponseEntity<?> getLastArchivedDailyDate(@PathVariable Integer subPortfolioId) {
        try {
            Optional<String> lastArchived = periodSnapshotService.getLastArchivedDailyDate(subPortfolioId.longValue());

            return ResponseEntity.ok(Map.of(
                "subPortfolioId", subPortfolioId,
                "lastArchivedDate", lastArchived.orElse(null),
                "hasArchivedData", lastArchived.isPresent()
            ));
        } catch (Exception e) {
            System.err.println("❌ [DailySnapshot] Error en getLastArchivedDailyDate: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
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

    public record DailyStatusResponse(
        Long subPortfolioId,
        String tableName,
        boolean hasExistingData,
        long recordCount,
        String lastLoadDate,
        String lastArchivedDate,
        boolean requiresConfirmation
    ) {}
}
