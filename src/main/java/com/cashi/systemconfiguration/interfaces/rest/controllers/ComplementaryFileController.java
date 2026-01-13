package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.shared.domain.model.entities.ComplementaryFileType;
import com.cashi.systemconfiguration.domain.services.ComplementaryFileService;
import com.cashi.systemconfiguration.interfaces.rest.resources.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller para gestión de archivos complementarios.
 * Los archivos complementarios actualizan columnas específicas de registros existentes
 * en la tabla de carga inicial.
 */
@RestController
@RequestMapping("/api/v1/system-config/complementary-files")
@Tag(name = "Complementary Files", description = "Gestión de archivos complementarios y sus tipos")
public class ComplementaryFileController {

    private final ComplementaryFileService complementaryFileService;

    public ComplementaryFileController(ComplementaryFileService complementaryFileService) {
        this.complementaryFileService = complementaryFileService;
    }

    // ==================== Importación de Datos Complementarios ====================

    @PostMapping("/import")
    @Operation(summary = "Importar datos desde archivo complementario",
               description = "Actualiza columnas específicas de registros existentes usando un campo de vinculación")
    public ResponseEntity<Map<String, Object>> importComplementaryData(
            @RequestBody ImportComplementaryResource resource) {

        Map<String, Object> result;

        if (resource.complementaryTypeId() != null) {
            result = complementaryFileService.importComplementaryData(
                    resource.subPortfolioId(),
                    resource.complementaryTypeId(),
                    resource.data());
        } else if (resource.typeName() != null && !resource.typeName().isBlank()) {
            result = complementaryFileService.importComplementaryDataByTypeName(
                    resource.subPortfolioId(),
                    resource.typeName(),
                    resource.data());
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Debe proporcionar complementaryTypeId o typeName"));
        }

        return ResponseEntity.ok(result);
    }

    // ==================== Detección de Tipo de Archivo ====================

    @PostMapping("/detect-type")
    @Operation(summary = "Detectar tipo de archivo por nombre",
               description = "Identifica el tipo de archivo complementario basándose en patrones configurados")
    public ResponseEntity<?> detectFileType(@RequestBody DetectFileTypeResource resource) {
        ComplementaryFileType detected = complementaryFileService.detectComplementaryType(
                resource.subPortfolioId(),
                resource.fileName());

        if (detected == null) {
            return ResponseEntity.ok(Map.of(
                    "detected", false,
                    "message", "El archivo no coincide con ningún tipo complementario configurado"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("detected", true);
        result.put("type", ComplementaryFileTypeResource.fromEntity(detected));

        return ResponseEntity.ok(result);
    }

    // ==================== CRUD de Tipos de Archivo Complementario ====================

    @GetMapping("/types/subportfolio/{subPortfolioId}")
    @Operation(summary = "Obtener tipos de archivo por subcartera")
    public ResponseEntity<List<ComplementaryFileTypeResource>> getTypesBySubPortfolio(
            @PathVariable Integer subPortfolioId) {

        List<ComplementaryFileType> types = complementaryFileService
                .getComplementaryTypesBySubPortfolio(subPortfolioId);

        List<ComplementaryFileTypeResource> resources = types.stream()
                .map(ComplementaryFileTypeResource::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(resources);
    }

    @GetMapping("/types/{id}")
    @Operation(summary = "Obtener tipo de archivo por ID")
    public ResponseEntity<ComplementaryFileTypeResource> getTypeById(@PathVariable Integer id) {
        ComplementaryFileType type = complementaryFileService.getComplementaryTypeById(id);
        return ResponseEntity.ok(ComplementaryFileTypeResource.fromEntity(type));
    }

    @PostMapping("/types")
    @Operation(summary = "Crear nuevo tipo de archivo complementario")
    public ResponseEntity<ComplementaryFileTypeResource> createType(
            @RequestBody CreateComplementaryTypeResource resource) {

        ComplementaryFileType created = complementaryFileService.createComplementaryType(
                resource.subPortfolioId(),
                resource.typeName(),
                resource.fileNamePattern(),
                resource.linkField(),
                resource.columnsToUpdate(),
                resource.description());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ComplementaryFileTypeResource.fromEntity(created));
    }

    @PutMapping("/types/{id}")
    @Operation(summary = "Actualizar tipo de archivo complementario")
    public ResponseEntity<ComplementaryFileTypeResource> updateType(
            @PathVariable Integer id,
            @RequestBody UpdateComplementaryTypeResource resource) {

        ComplementaryFileType updated = complementaryFileService.updateComplementaryType(
                id,
                resource.typeName(),
                resource.fileNamePattern(),
                resource.linkField(),
                resource.columnsToUpdate(),
                resource.description(),
                resource.isActive());

        return ResponseEntity.ok(ComplementaryFileTypeResource.fromEntity(updated));
    }

    @DeleteMapping("/types/{id}")
    @Operation(summary = "Eliminar tipo de archivo complementario")
    public ResponseEntity<Void> deleteType(@PathVariable Integer id) {
        complementaryFileService.deleteComplementaryType(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Validación ====================

    @PostMapping("/validate-columns")
    @Operation(summary = "Validar que las columnas existan en la configuración",
               description = "Retorna la lista de columnas que NO existen en la tabla de carga inicial")
    public ResponseEntity<Map<String, Object>> validateColumns(
            @RequestParam Integer subPortfolioId,
            @RequestBody List<String> columns) {

        List<String> missingColumns = complementaryFileService.validateColumnsExist(subPortfolioId, columns);

        Map<String, Object> result = new HashMap<>();
        result.put("valid", missingColumns.isEmpty());
        result.put("missingColumns", missingColumns);

        return ResponseEntity.ok(result);
    }
}
