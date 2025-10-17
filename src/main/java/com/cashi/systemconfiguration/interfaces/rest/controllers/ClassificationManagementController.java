package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.systemconfiguration.application.internal.commandservices.ClassificationCommandServiceImpl;
import com.cashi.systemconfiguration.application.internal.queryservices.ClassificationQueryServiceImpl;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationCatalog;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationConfigHistory;
import com.cashi.systemconfiguration.domain.model.entities.ConfigurationVersion;
import com.cashi.systemconfiguration.domain.model.entities.TenantClassificationConfig;
import com.cashi.systemconfiguration.interfaces.rest.resources.*;
import com.cashi.systemconfiguration.interfaces.rest.transform.ClassificationResourceAssembler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class ClassificationManagementController {

    private final ClassificationCommandServiceImpl commandService;
    private final ClassificationQueryServiceImpl queryService;

    public ClassificationManagementController(
            ClassificationCommandServiceImpl commandService,
            ClassificationQueryServiceImpl queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    // ========================================
    // CATALOG MANAGEMENT (System-wide)
    // ========================================

    @GetMapping("/classifications")
    public ResponseEntity<List<ClassificationCatalogResource>> getAllClassifications() {
        List<ClassificationCatalog> classifications = queryService.getAllActiveClassifications();
        List<ClassificationCatalogResource> resources = classifications.stream()
            .map(ClassificationResourceAssembler::toResourceFromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/classifications/type/{type}")
    public ResponseEntity<List<ClassificationCatalogResource>> getClassificationsByType(
            @PathVariable String type) {
        ClassificationCatalog.ClassificationType classificationType =
            ClassificationCatalog.ClassificationType.valueOf(type);
        List<ClassificationCatalog> classifications = queryService.getClassificationsByType(classificationType);
        List<ClassificationCatalogResource> resources = classifications.stream()
            .map(ClassificationResourceAssembler::toResourceFromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/classifications/type/{type}/root")
    public ResponseEntity<List<ClassificationCatalogResource>> getRootClassifications(
            @PathVariable String type) {
        ClassificationCatalog.ClassificationType classificationType =
            ClassificationCatalog.ClassificationType.valueOf(type);
        List<ClassificationCatalog> classifications = queryService.getRootClassificationsByType(classificationType);
        List<ClassificationCatalogResource> resources = classifications.stream()
            .map(ClassificationResourceAssembler::toResourceFromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/classifications/{parentId}/children")
    public ResponseEntity<List<ClassificationCatalogResource>> getChildClassifications(
            @PathVariable Long parentId) {
        List<ClassificationCatalog> classifications = queryService.getChildClassifications(parentId);
        List<ClassificationCatalogResource> resources = classifications.stream()
            .map(ClassificationResourceAssembler::toResourceFromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/classifications/{id}")
    public ResponseEntity<ClassificationCatalogResource> getClassificationById(@PathVariable Long id) {
        return queryService.getClassificationById(id)
            .map(ClassificationResourceAssembler::toResourceFromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/classifications")
    public ResponseEntity<ClassificationCatalogResource> createClassification(
            @Valid @RequestBody CreateClassificationCommand command,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        ClassificationCatalog.ClassificationType type =
            ClassificationCatalog.ClassificationType.valueOf(command.classificationType());

        ClassificationCatalog created = commandService.createClassification(
            command.code(),
            command.name(),
            type,
            command.parentClassificationId(),
            command.description(),
            command.displayOrder(),
            command.iconName(),
            command.colorHex(),
            command.metadataSchema(),
            userId
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ClassificationResourceAssembler.toResourceFromEntity(created));
    }

    @PutMapping("/classifications/{id}")
    public ResponseEntity<ClassificationCatalogResource> updateClassification(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassificationCommand command,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        ClassificationCatalog updated = commandService.updateClassification(
            id,
            command.name(),
            command.description(),
            command.iconName(),
            command.colorHex(),
            command.displayOrder(),
            command.isActive() != null ? command.isActive() : true,
            command.metadataSchema(),
            userId
        );
        return ResponseEntity.ok(ClassificationResourceAssembler.toResourceFromEntity(updated));
    }

    @DeleteMapping("/classifications/{id}")
    public ResponseEntity<Void> deleteClassification(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        commandService.deleteClassification(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/classifications/display-order")
    public ResponseEntity<Void> updateDisplayOrder(
            @RequestBody List<DisplayOrderUpdateCommand> updates,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        commandService.updateDisplayOrder(updates, userId);
        return ResponseEntity.ok().build();
    }

    // ========================================
    // TENANT/PORTFOLIO CONFIGURATION
    // ========================================

    @GetMapping("/tenants/{tenantId}/classifications")
    public ResponseEntity<List<TenantClassificationConfigResource>> getTenantClassifications(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        List<TenantClassificationConfig> configs = includeDisabled
            ? queryService.getAllTenantClassifications(tenantId, portfolioId)
            : queryService.getEnabledClassifications(tenantId, portfolioId);
        List<TenantClassificationConfigResource> resources = configs.stream()
            .map(ClassificationResourceAssembler::toResourceFromConfig)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/tenants/{tenantId}/classifications/type/{type}")
    public ResponseEntity<List<TenantClassificationConfigResource>> getTenantClassificationsByType(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId,
            @PathVariable String type) {
        ClassificationCatalog.ClassificationType classificationType =
            ClassificationCatalog.ClassificationType.valueOf(type);

        List<TenantClassificationConfig> configs =
            queryService.getEnabledClassificationsByType(tenantId, portfolioId, classificationType);
        List<TenantClassificationConfigResource> resources = configs.stream()
            .map(ClassificationResourceAssembler::toResourceFromConfig)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/tenants/{tenantId}/classifications/level/{level}")
    public ResponseEntity<List<TenantClassificationConfigResource>> getTenantClassificationsByLevel(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId,
            @PathVariable Integer level) {
        List<TenantClassificationConfig> configs =
            queryService.getEnabledClassificationsByLevel(tenantId, portfolioId, level);
        List<TenantClassificationConfigResource> resources = configs.stream()
            .map(ClassificationResourceAssembler::toResourceFromConfig)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/tenants/{tenantId}/classifications/{parentId}/children")
    public ResponseEntity<List<TenantClassificationConfigResource>> getTenantChildClassifications(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId,
            @PathVariable Long parentId) {
        List<TenantClassificationConfig> configs =
            queryService.getChildClassificationsByParent(tenantId, portfolioId, parentId);
        List<TenantClassificationConfigResource> resources = configs.stream()
            .map(ClassificationResourceAssembler::toResourceFromConfig)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @PostMapping("/tenants/{tenantId}/classifications/{classificationId}/enable")
    public ResponseEntity<TenantClassificationConfigResource> enableClassification(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId,
            @PathVariable Long classificationId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        TenantClassificationConfig config = commandService.enableClassification(
            tenantId, portfolioId, classificationId, userId
        );
        return ResponseEntity.ok(ClassificationResourceAssembler.toResourceFromConfig(config));
    }

    @PostMapping("/tenants/{tenantId}/classifications/{classificationId}/disable")
    public ResponseEntity<TenantClassificationConfigResource> disableClassification(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId,
            @PathVariable Long classificationId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        TenantClassificationConfig config = commandService.disableClassification(
            tenantId, portfolioId, classificationId, userId
        );
        return ResponseEntity.ok(ClassificationResourceAssembler.toResourceFromConfig(config));
    }

    @PutMapping("/tenants/{tenantId}/classifications/{classificationId}/config")
    public ResponseEntity<TenantClassificationConfigResource> updateClassificationConfig(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId,
            @PathVariable Long classificationId,
            @Valid @RequestBody UpdateClassificationConfigCommand command,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        TenantClassificationConfig config = commandService.updateClassificationConfig(
            tenantId, portfolioId, classificationId,
            command.customName(), command.customOrder(),
            command.customIcon(), command.customColor(),
            command.requiresComment(), userId
        );
        return ResponseEntity.ok(ClassificationResourceAssembler.toResourceFromConfig(config));
    }

    // ========================================
    // VERSIONING & SNAPSHOTS
    // ========================================

    @GetMapping("/tenants/{tenantId}/configuration-versions")
    public ResponseEntity<List<ConfigurationVersion>> getVersionHistory(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId) {
        List<ConfigurationVersion> versions = queryService.getVersionHistory(tenantId, portfolioId);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/tenants/{tenantId}/configuration-versions/active")
    public ResponseEntity<ConfigurationVersion> getActiveVersion(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId) {
        return queryService.getActiveVersion(tenantId, portfolioId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/tenants/{tenantId}/configuration-versions/snapshot")
    public ResponseEntity<ConfigurationVersion> createSnapshot(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId,
            @Valid @RequestBody CreateSnapshotCommand command,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        ConfigurationVersion version = commandService.createSnapshot(
            tenantId, portfolioId, command.versionName(), command.description(), userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }

    @PostMapping("/tenants/{tenantId}/configuration-versions/{versionId}/activate")
    public ResponseEntity<Void> activateVersion(
            @PathVariable Long tenantId,
            @PathVariable Long versionId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        commandService.activateVersion(versionId, userId);
        return ResponseEntity.ok().build();
    }

    // ========================================
    // AUDIT & HISTORY
    // ========================================

    @GetMapping("/tenants/{tenantId}/audit/changes")
    public ResponseEntity<Page<ClassificationConfigHistory>> getChangeHistory(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId,
            Pageable pageable) {
        Page<ClassificationConfigHistory> history = queryService.getChangeHistory(tenantId, portfolioId, pageable);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/audit/entity/{entityType}/{entityId}")
    public ResponseEntity<List<ClassificationConfigHistory>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        ClassificationConfigHistory.EntityType type =
            ClassificationConfigHistory.EntityType.valueOf(entityType);
        List<ClassificationConfigHistory> history = queryService.getEntityHistory(type, entityId);
        return ResponseEntity.ok(history);
    }

    // ========================================
    // CLASSIFICATION FIELDS (Dynamic Forms)
    // ========================================

    /**
     * Obtiene los campos dinámicos configurados para una clasificación específica
     * Solo las clasificaciones "hoja" (sin hijos) deberían tener campos asociados
     */
    @GetMapping("/tenants/{tenantId}/classifications/{classificationId}/fields")
    public ResponseEntity<ClassificationFieldsResponse> getClassificationFields(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long portfolioId,
            @PathVariable Long classificationId) {

        boolean isLeaf = queryService.isLeafClassification(tenantId, portfolioId, classificationId);
        List<ClassificationFieldResource> fieldResources = new ArrayList<>();

        // Primero intentar obtener desde field mappings (tabla mapeos_campo_clasificacion)
        var mappedFields = queryService.getVisibleClassificationFields(tenantId, portfolioId, classificationId);

        if (!mappedFields.isEmpty()) {
            // Si hay mappings configurados, usarlos
            fieldResources = mappedFields.stream()
                .map(this::toFieldResource)
                .collect(Collectors.toList());
        } else {
            // Si no hay mappings, intentar obtener desde metadataSchema del catálogo
            var classificationOpt = queryService.getClassificationById(classificationId);
            if (classificationOpt.isPresent() && classificationOpt.get().getMetadataSchema() != null) {
                fieldResources = parseMetadataSchemaToFields(classificationOpt.get().getMetadataSchema());
            }
        }

        var response = new ClassificationFieldsResponse(
            classificationId,
            isLeaf,
            fieldResources
        );

        return ResponseEntity.ok(response);
    }

    private List<ClassificationFieldResource> parseMetadataSchemaToFields(String metadataSchemaJson) {
        List<ClassificationFieldResource> fields = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(metadataSchemaJson);
            JsonNode fieldsNode = root.get("fields");

            if (fieldsNode != null && fieldsNode.isArray()) {
                int index = 0;
                for (JsonNode fieldNode : fieldsNode) {
                    // Soportar ambos formatos: estándar (id/label/type) y tenant config (fieldCode/fieldName/fieldType)
                    String id = fieldNode.has("fieldCode") ? fieldNode.get("fieldCode").asText() :
                               (fieldNode.has("id") ? fieldNode.get("id").asText() : "field_" + index);
                    String label = fieldNode.has("fieldName") ? fieldNode.get("fieldName").asText() :
                                  (fieldNode.has("label") ? fieldNode.get("label").asText() : "Campo");
                    // Mantener en lowercase para coincidir con los typeCode en la tabla field_types
                    String type = fieldNode.has("fieldType") ? fieldNode.get("fieldType").asText().toLowerCase() :
                                 (fieldNode.has("type") ? fieldNode.get("type").asText().toLowerCase() : "text");
                    boolean required = (fieldNode.has("isRequired") && fieldNode.get("isRequired").asBoolean()) ||
                                      (fieldNode.has("required") && fieldNode.get("required").asBoolean());
                    String placeholder = fieldNode.has("placeholder") ? fieldNode.get("placeholder").asText() : null;
                    String helpText = fieldNode.has("description") ? fieldNode.get("description").asText() :
                                     (fieldNode.has("helpText") ? fieldNode.get("helpText").asText() : null);
                    int displayOrder = fieldNode.has("displayOrder") ? fieldNode.get("displayOrder").asInt() : index;

                    // Parse options for SELECT type
                    List<String> options = null;
                    if ("SELECT".equalsIgnoreCase(type) && fieldNode.has("options")) {
                        options = new ArrayList<>();
                        JsonNode optionsNode = fieldNode.get("options");
                        if (optionsNode.isArray()) {
                            for (JsonNode optNode : optionsNode) {
                                options.add(optNode.asText());
                            }
                        }
                    }

                    // Parse validationRules
                    String validationRules = null;
                    if (fieldNode.has("validationRules")) {
                        validationRules = fieldNode.get("validationRules").toString();
                    }

                    // Parse columns for TABLE type
                    List<TableColumnResource> columns = null;
                    Integer minRows = null;
                    Integer maxRows = null;
                    Boolean allowAddRow = null;
                    Boolean allowDeleteRow = null;

                    if ("TABLE".equalsIgnoreCase(type) && fieldNode.has("columns")) {
                        columns = new ArrayList<>();
                        JsonNode columnsNode = fieldNode.get("columns");
                        if (columnsNode.isArray()) {
                            for (JsonNode colNode : columnsNode) {
                                String colId = colNode.has("id") ? colNode.get("id").asText() : "col_" + columns.size();
                                String colLabel = colNode.has("label") ? colNode.get("label").asText() : "Columna";
                                // Mantener en lowercase para coincidir con los typeCode en la tabla field_types
                                String colType = colNode.has("type") ? colNode.get("type").asText().toLowerCase() : "text";
                                boolean colRequired = colNode.has("required") && colNode.get("required").asBoolean();

                                // Parse options para columnas tipo select
                                List<String> colOptions = null;
                                if ("select".equalsIgnoreCase(colType) && colNode.has("options")) {
                                    colOptions = new ArrayList<>();
                                    JsonNode colOptionsNode = colNode.get("options");
                                    if (colOptionsNode.isArray()) {
                                        for (JsonNode optNode : colOptionsNode) {
                                            colOptions.add(optNode.asText());
                                        }
                                    }
                                }

                                columns.add(new TableColumnResource(colId, colLabel, colType, colRequired, colOptions));
                            }
                        }

                        minRows = fieldNode.has("minRows") ? fieldNode.get("minRows").asInt() : 0;
                        maxRows = fieldNode.has("maxRows") ? fieldNode.get("maxRows").asInt() : null;
                        allowAddRow = fieldNode.has("allowAddRow") ? fieldNode.get("allowAddRow").asBoolean() : true;
                        allowDeleteRow = fieldNode.has("allowDeleteRow") ? fieldNode.get("allowDeleteRow").asBoolean() : true;
                    }

                    // Extract linkedToField for bidirectional number↔table synchronization
                    String linkedToField = fieldNode.has("linkedToField") ? fieldNode.get("linkedToField").asText() : null;

                    fields.add(new ClassificationFieldResource(
                        (long) index,
                        id,
                        label,
                        type,
                        null, // category
                        helpText,
                        null, // defaultValue
                        validationRules,
                        required,
                        true, // visible
                        displayOrder,
                        null,  // conditionalLogic
                        options,
                        columns,
                        minRows,
                        maxRows,
                        allowAddRow,
                        allowDeleteRow,
                        linkedToField
                    ));
                    index++;
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing metadataSchema: " + e.getMessage());
            e.printStackTrace();
        }
        return fields;
    }

    private ClassificationFieldResource toFieldResource(com.cashi.systemconfiguration.domain.model.entities.ClassificationFieldMapping mapping) {
        var fieldDef = mapping.getFieldDefinition();
        return new ClassificationFieldResource(
            fieldDef.getId(),
            fieldDef.getFieldCode(),
            fieldDef.getFieldName(),
            fieldDef.getFieldType().name(),
            fieldDef.getFieldCategory(),
            fieldDef.getDescription(),
            fieldDef.getDefaultValue(),
            fieldDef.getValidationRules(),
            mapping.getIsRequired(),
            mapping.getIsVisible(),
            mapping.getDisplayOrder(),
            mapping.getConditionalLogic(),
            null,  // options - TODO: parse from fieldDef if needed
            null,  // columns - TODO: implement from mapping if needed
            null,  // minRows
            null,  // maxRows
            null,  // allowAddRow
            null,  // allowDeleteRow
            null   // linkedToField - TODO: implement if needed in field mappings
        );
    }

    // Response DTOs (inner records for convenience)
    public record ClassificationFieldsResponse(
        Long classificationId,
        boolean isLeaf,
        List<ClassificationFieldResource> fields
    ) {}

    public record ClassificationFieldResource(
        Long id,
        String fieldCode,
        String fieldName,
        String fieldType,
        String fieldCategory,
        String description,
        String defaultValue,
        String validationRules,
        Boolean isRequired,
        Boolean isVisible,
        Integer displayOrder,
        String conditionalLogic,
        List<String> options,  // Para campos tipo SELECT
        List<TableColumnResource> columns,  // Para campos tipo TABLE
        Integer minRows,
        Integer maxRows,
        Boolean allowAddRow,
        Boolean allowDeleteRow,
        String linkedToField  // Para sincronización bidireccional número↔tabla
    ) {}

    public record TableColumnResource(
        String id,
        String label,
        String type,
        Boolean required,
        List<String> options  // Para columnas tipo select
    ) {}
}
