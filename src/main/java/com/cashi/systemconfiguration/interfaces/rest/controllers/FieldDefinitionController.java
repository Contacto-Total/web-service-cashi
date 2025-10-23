package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.systemconfiguration.domain.services.FieldDefinitionQueryService;
import com.cashi.systemconfiguration.interfaces.rest.resources.FieldDefinitionResource;
import com.cashi.systemconfiguration.interfaces.rest.transform.FieldDefinitionResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system-config/field-definitions")
@Tag(name = "Field Definition", description = "Catálogo Maestro de Campos del Sistema")
public class FieldDefinitionController {

    private final FieldDefinitionQueryService queryService;

    public FieldDefinitionController(FieldDefinitionQueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(summary = "Obtener todas las definiciones de campos activas")
    @GetMapping
    public ResponseEntity<List<FieldDefinitionResource>> getAllActive() {
        var definitions = queryService.getAllActive();
        var resources = definitions.stream()
                .map(FieldDefinitionResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Obtener definiciones de campos por categoría")
    @GetMapping("/category/{category}")
    public ResponseEntity<List<FieldDefinitionResource>> getByCategory(@PathVariable String category) {
        var definitions = queryService.getAllActiveByCategory(category);
        var resources = definitions.stream()
                .map(FieldDefinitionResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Obtener definiciones de campos por tipo de dato")
    @GetMapping("/data-type/{dataType}")
    public ResponseEntity<List<FieldDefinitionResource>> getByDataType(@PathVariable String dataType) {
        try {
            var definitions = queryService.getAllActiveByDataType(dataType);
            var resources = definitions.stream()
                    .map(FieldDefinitionResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();
            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Obtener definición de campo por ID")
    @GetMapping("/{id}")
    public ResponseEntity<FieldDefinitionResource> getById(@PathVariable Integer id) {
        var definition = queryService.getById(id);
        return definition.map(d -> ResponseEntity.ok(
                FieldDefinitionResourceFromEntityAssembler.toResourceFromEntity(d)
        )).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener definición de campo por código")
    @GetMapping("/code/{fieldCode}")
    public ResponseEntity<FieldDefinitionResource> getByFieldCode(@PathVariable String fieldCode) {
        var definition = queryService.getByFieldCode(fieldCode);
        return definition.map(d -> ResponseEntity.ok(
                FieldDefinitionResourceFromEntityAssembler.toResourceFromEntity(d)
        )).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Contar campos activos")
    @GetMapping("/count")
    public ResponseEntity<Long> countActiveFields() {
        long count = queryService.countActiveFields();
        return ResponseEntity.ok(count);
    }
}
