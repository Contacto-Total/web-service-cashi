package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.shared.application.services.FieldTypeService;
import com.cashi.shared.domain.model.entities.FieldTypeCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller para gestionar el catálogo de tipos de campo
 */
@Tag(name = "Field Types", description = "API para gestionar tipos de campo disponibles")
@RestController
@RequestMapping("/api/v1/field-types")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FieldTypeController {

    private final FieldTypeService fieldTypeService;

    public FieldTypeController(FieldTypeService fieldTypeService) {
        this.fieldTypeService = fieldTypeService;
    }

    /**
     * Obtiene todos los tipos de campo activos
     */
    @Operation(summary = "Obtener todos los tipos de campo", description = "Retorna todos los tipos de campo activos ordenados por displayOrder")
    @GetMapping
    public ResponseEntity<List<FieldTypeResource>> getAllFieldTypes() {
        List<FieldTypeCatalog> fieldTypes = fieldTypeService.getAllActiveFieldTypes();
        return ResponseEntity.ok(fieldTypes.stream()
                .map(this::toResource)
                .collect(Collectors.toList()));
    }

    /**
     * Obtiene tipos de campo disponibles para campos principales
     */
    @Operation(summary = "Obtener tipos para campos principales", description = "Retorna tipos de campo disponibles para configurar como campos principales")
    @GetMapping("/main-fields")
    public ResponseEntity<List<FieldTypeResource>> getFieldTypesForMainFields() {
        List<FieldTypeCatalog> fieldTypes = fieldTypeService.getFieldTypesForMainFields();
        return ResponseEntity.ok(fieldTypes.stream()
                .map(this::toResource)
                .collect(Collectors.toList()));
    }

    /**
     * Obtiene tipos de campo disponibles para columnas de tabla
     */
    @Operation(summary = "Obtener tipos para columnas de tabla", description = "Retorna tipos de campo disponibles para configurar como columnas dentro de campos tipo tabla")
    @GetMapping("/table-columns")
    public ResponseEntity<List<FieldTypeResource>> getFieldTypesForTableColumns() {
        List<FieldTypeCatalog> fieldTypes = fieldTypeService.getFieldTypesForTableColumns();
        return ResponseEntity.ok(fieldTypes.stream()
                .map(this::toResource)
                .collect(Collectors.toList()));
    }

    /**
     * Convierte entidad a DTO
     */
    private FieldTypeResource toResource(FieldTypeCatalog fieldType) {
        return new FieldTypeResource(
                fieldType.getId(),
                fieldType.getTypeCode(),
                fieldType.getTypeName(),
                fieldType.getDescription(),
                fieldType.getIcon(),
                fieldType.getAvailableForMainField(),
                fieldType.getAvailableForTableColumn(),
                fieldType.getDisplayOrder()
        );
    }

    /**
     * DTO para tipos de campo
     */
    public record FieldTypeResource(
            Long id,
            String typeCode,
            String typeName,
            String description,
            String icon,
            Boolean availableForMainField,
            Boolean availableForTableColumn,
            Integer displayOrder
    ) {}
}
