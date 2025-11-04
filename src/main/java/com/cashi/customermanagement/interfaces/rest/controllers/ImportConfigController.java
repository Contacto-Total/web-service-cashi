package com.cashi.customermanagement.interfaces.rest.controllers;

import com.cashi.customermanagement.application.services.ImportConfigService;
import com.cashi.customermanagement.application.services.FileWatcherService;
import com.cashi.customermanagement.interfaces.rest.resources.*;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/import-config")
@Tag(name = "Import Configuration", description = "Configuración de importación automática de archivos")
@CrossOrigin(origins = "*")
public class ImportConfigController {

    private final ImportConfigService importConfigService;
    private final FileWatcherService fileWatcherService;

    public ImportConfigController(ImportConfigService importConfigService,
                                 FileWatcherService fileWatcherService) {
        this.importConfigService = importConfigService;
        this.fileWatcherService = fileWatcherService;
    }

    @Operation(summary = "Obtener configuración actual", description = "Retorna la configuración de importación automática")
    @GetMapping
    public ResponseEntity<ImportConfigResource> getConfig() {
        ImportConfigResource config = importConfigService.getConfig();
        return ResponseEntity.ok(config);
    }

    @Operation(summary = "Guardar configuración", description = "Guarda o actualiza la configuración de importación automática")
    @PostMapping
    public ResponseEntity<ImportConfigResource> saveConfig(@RequestBody ImportConfigRequest request) {
        try {
            ImportConfigResource saved = importConfigService.saveConfig(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Obtener historial", description = "Retorna el historial de archivos procesados")
    @GetMapping("/history")
    public ResponseEntity<List<ImportHistoryResource>> getHistory(
            @Parameter(description = "ID de subcartera (opcional)")
            @RequestParam(required = false) Long subPortfolioId) {
        List<ImportHistoryResource> history = importConfigService.getHistory(subPortfolioId);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Escanear carpeta", description = "Escanea la carpeta y retorna los archivos que coinciden con el patrón")
    @GetMapping("/scan")
    public ResponseEntity<?> scanFolder(
            @Parameter(description = "Directorio a escanear")
            @RequestParam String watchDirectory,
            @Parameter(description = "Patrón de archivo")
            @RequestParam String filePattern) {
        try {
            List<FilePreviewResource> files = importConfigService.scanFolder(watchDirectory, filePattern);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Activar/Desactivar monitoreo", description = "Activa o desactiva el monitoreo automático")
    @PostMapping("/toggle")
    public ResponseEntity<?> toggleMonitoring(@RequestParam Boolean active) {
        try {
            // Obtener configuración actual
            ImportConfigResource current = importConfigService.getConfig();

            // Crear request con el estado actualizado
            ImportConfigRequest request = new ImportConfigRequest(
                    current.watchDirectory(),
                    current.filePattern(),
                    current.subPortfolioId(),
                    current.scheduledTime(),
                    active,
                    current.processedDirectory(),
                    current.errorDirectory(),
                    current.moveAfterProcess()
            );

            ImportConfigResource updated = importConfigService.saveConfig(request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Validar cabeceras de archivo", description = "Valida que las cabeceras del archivo coincidan con la configuración")
    @PostMapping("/validate-headers")
    public ResponseEntity<?> validateHeaders(@RequestBody HeaderValidationRequest request) {
        try {
            LoadType loadType = LoadType.valueOf(request.loadType());
            HeaderValidationResource result = importConfigService.validateFileHeaders(
                    request.filePath(),
                    request.subPortfolioId(),
                    loadType
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al validar cabeceras: " + e.getMessage()));
        }
    }

    @Operation(summary = "Trigger manual de importación", description = "Ejecuta la importación inmediatamente sin esperar la hora programada. Retorna errores de validación para mostrar en log.")
    @PostMapping("/trigger-import")
    public ResponseEntity<?> triggerManualImport() {
        try {
            Map<String, Object> result = fileWatcherService.triggerManualImport();

            // Si hay errores, retornar con status 200 pero indicando que hay errores
            // para que el frontend pueda mostrarlos en el log rojo
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error inesperado: " + e.getMessage(),
                        "errors", List.of(e.getMessage())
                    ));
        }
    }
}
