package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.shared.domain.model.valueobjects.LoadType;
import com.cashi.systemconfiguration.domain.services.HeaderConfigurationCommandService;
import com.cashi.systemconfiguration.domain.services.HeaderConfigurationQueryService;
import com.cashi.systemconfiguration.interfaces.rest.resources.*;
import com.cashi.systemconfiguration.interfaces.rest.transform.HeaderConfigurationResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/system-config/header-configurations")
@Tag(name = "Header Configuration", description = "Configuración de Cabeceras Personalizadas")
public class HeaderConfigurationController {

    private final HeaderConfigurationCommandService commandService;
    private final HeaderConfigurationQueryService queryService;

    public HeaderConfigurationController(
            HeaderConfigurationCommandService commandService,
            HeaderConfigurationQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @Operation(summary = "Obtener configuraciones de cabeceras por subcartera")
    @GetMapping("/subportfolio/{subPortfolioId}")
    public ResponseEntity<List<HeaderConfigurationResource>> getBySubPortfolio(
            @PathVariable Integer subPortfolioId) {
        try {
            var configs = queryService.getAllBySubPortfolio(subPortfolioId);
            var resources = configs.stream()
                    .map(HeaderConfigurationResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();
            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Obtener configuraciones de cabeceras por subcartera y tipo de carga")
    @GetMapping("/subportfolio/{subPortfolioId}/load-type/{loadType}")
    public ResponseEntity<List<HeaderConfigurationResource>> getBySubPortfolioAndLoadType(
            @PathVariable Integer subPortfolioId,
            @PathVariable LoadType loadType) {
        try {
            var configs = queryService.getAllBySubPortfolioAndLoadType(subPortfolioId, loadType);
            var resources = configs.stream()
                    .map(HeaderConfigurationResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();
            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Obtener configuración de cabecera por ID")
    @GetMapping("/{id}")
    public ResponseEntity<HeaderConfigurationResource> getById(@PathVariable Integer id) {
        var config = queryService.getById(id);
        return config.map(c -> ResponseEntity.ok(
                HeaderConfigurationResourceFromEntityAssembler.toResourceFromEntity(c)
        )).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear nueva configuración de cabecera")
    @PostMapping
    public ResponseEntity<HeaderConfigurationResource> create(
            @RequestBody CreateHeaderConfigurationResource resource) {
        try {
            var config = commandService.createHeaderConfiguration(
                    resource.subPortfolioId(),
                    resource.fieldDefinitionId(),
                    resource.headerName(),
                    resource.displayLabel(),
                    resource.format(),
                    resource.required(),
                    resource.loadType()
            );
            var responseResource = HeaderConfigurationResourceFromEntityAssembler.toResourceFromEntity(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Crear múltiples configuraciones en lote (importar CSV)")
    @PostMapping("/bulk")
    public ResponseEntity<List<HeaderConfigurationResource>> createBulk(
            @RequestBody BulkCreateHeaderConfigurationResource resource) {
        try {
            var dataList = resource.headers().stream()
                    .map(h -> new HeaderConfigurationCommandService.HeaderConfigurationData(
                            h.fieldDefinitionId(),
                            h.headerName(),
                            h.dataType(),
                            h.displayLabel(),
                            h.format(),
                            h.required()
                    ))
                    .toList();

            var configs = commandService.createBulkHeaderConfigurations(
                    resource.subPortfolioId(),
                    resource.loadType(),
                    dataList
            );

            var resources = configs.stream()
                    .map(HeaderConfigurationResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();

            return ResponseEntity.status(HttpStatus.CREATED).body(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Actualizar configuración de cabecera")
    @PutMapping("/{id}")
    public ResponseEntity<HeaderConfigurationResource> update(
            @PathVariable Integer id,
            @RequestBody UpdateHeaderConfigurationResource resource) {
        try {
            var config = commandService.updateHeaderConfiguration(
                    id,
                    resource.displayLabel(),
                    resource.format(),
                    resource.required(),
                    resource.loadType()
            );
            var responseResource = HeaderConfigurationResourceFromEntityAssembler.toResourceFromEntity(config);
            return ResponseEntity.ok(responseResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Eliminar configuración de cabecera")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            commandService.deleteHeaderConfiguration(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Eliminar todas las configuraciones de una subcartera y tipo de carga")
    @DeleteMapping("/subportfolio/{subPortfolioId}/load-type/{loadType}")
    public ResponseEntity<Void> deleteAllBySubPortfolioAndLoadType(
            @PathVariable Integer subPortfolioId,
            @PathVariable LoadType loadType) {
        try {
            commandService.deleteAllBySubPortfolioAndLoadType(subPortfolioId, loadType);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Contar configuraciones de una subcartera")
    @GetMapping("/subportfolio/{subPortfolioId}/count")
    public ResponseEntity<Long> countBySubPortfolio(@PathVariable Integer subPortfolioId) {
        try {
            var count = queryService.countBySubPortfolio(subPortfolioId);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Importar datos masivos a la tabla dinámica")
    @PostMapping("/subportfolio/{subPortfolioId}/import-data")
    public ResponseEntity<?> importData(
            @PathVariable Integer subPortfolioId,
            @RequestBody ImportDataResource resource) {
        try {
            var result = commandService.importDataToTable(subPortfolioId, resource.loadType(), resource.data());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al importar datos: " + e.getMessage()));
        }
    }
}
