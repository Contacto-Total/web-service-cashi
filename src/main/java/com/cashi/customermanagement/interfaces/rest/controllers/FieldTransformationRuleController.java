package com.cashi.customermanagement.interfaces.rest.controllers;

import com.cashi.customermanagement.domain.model.entities.FieldTransformationRule;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.FieldTransformationRuleRepository;
import com.cashi.shared.domain.model.entities.FieldDefinition;
import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.FieldDefinitionRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.HeaderConfigurationRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/field-transformation-rules")
@Tag(name = "Field Transformation Rules", description = "Gestión de reglas de transformación de campos para importación")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FieldTransformationRuleController {

    private final FieldTransformationRuleRepository repository;
    private final HeaderConfigurationRepository headerConfigRepository;
    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final SubPortfolioRepository subPortfolioRepository;

    @Operation(summary = "Obtener todas las reglas de un tenant",
               description = "Retorna todas las reglas de transformación configuradas para un tenant")
    @ApiResponse(responseCode = "200", description = "Reglas obtenidas exitosamente")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<FieldTransformationRule>> getRulesByTenant(
            @Parameter(description = "ID del tenant") @PathVariable Long tenantId) {

        List<FieldTransformationRule> rules = repository.findByTenantIdOrderByRuleOrderAsc(tenantId);
        return ResponseEntity.ok(rules);
    }

    @Operation(summary = "Obtener reglas activas de un tenant",
               description = "Retorna solo las reglas activas de transformación para un tenant")
    @ApiResponse(responseCode = "200", description = "Reglas activas obtenidas exitosamente")
    @GetMapping("/tenant/{tenantId}/active")
    public ResponseEntity<List<FieldTransformationRule>> getActiveRulesByTenant(
            @Parameter(description = "ID del tenant") @PathVariable Long tenantId) {

        List<FieldTransformationRule> rules = repository
                .findByTenantIdAndIsActiveTrueOrderByRuleOrderAsc(tenantId);
        return ResponseEntity.ok(rules);
    }

    @Operation(summary = "Obtener reglas para un campo específico",
               description = "Retorna las reglas de transformación activas para un campo destino")
    @ApiResponse(responseCode = "200", description = "Reglas obtenidas exitosamente")
    @GetMapping("/tenant/{tenantId}/field/{targetField}")
    public ResponseEntity<List<FieldTransformationRule>> getRulesForField(
            @Parameter(description = "ID del tenant") @PathVariable Long tenantId,
            @Parameter(description = "Campo destino", example = "documento") @PathVariable String targetField) {

        List<FieldTransformationRule> rules = repository
                .findByTenantIdAndTargetFieldAndIsActiveTrueOrderByRuleOrderAsc(tenantId, targetField);
        return ResponseEntity.ok(rules);
    }

    @Operation(summary = "Crear nueva regla de transformación",
               description = "Crea una nueva regla de transformación de campos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Regla creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<?> createRule(@RequestBody FieldTransformationRule rule) {
        try {
            // Validaciones básicas
            if (rule.getTenantId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "TenantId es requerido"));
            }
            if (rule.getTargetField() == null || rule.getTargetField().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "TargetField es requerido"));
            }
            if (rule.getSourceField() == null || rule.getSourceField().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "SourceField es requerido"));
            }
            if (rule.getTargetHeaderName() == null || rule.getTargetHeaderName().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "TargetHeaderName es requerido"));
            }
            if (rule.getSubPortfolioId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "SubPortfolioId es requerido"));
            }

            // Establecer valores por defecto
            if (rule.getIsActive() == null) {
                rule.setIsActive(true);
            }
            if (rule.getRuleOrder() == null) {
                // Obtener el máximo orden actual y agregar 1
                List<FieldTransformationRule> existingRules = repository
                        .findByTenantIdOrderByRuleOrderAsc(rule.getTenantId());
                int maxOrder = existingRules.stream()
                        .mapToInt(r -> r.getRuleOrder() != null ? r.getRuleOrder() : 0)
                        .max()
                        .orElse(0);
                rule.setRuleOrder(maxOrder + 1);
            }

            // Guardar la regla primero
            FieldTransformationRule savedRule = repository.save(rule);

            // Crear automáticamente la cabecera en configuracion_cabeceras
            try {
                // Buscar el FieldDefinition por fieldCode
                FieldDefinition fieldDef = fieldDefinitionRepository.findByFieldCode(rule.getTargetField())
                        .orElseThrow(() -> new RuntimeException("Campo destino no encontrado: " + rule.getTargetField()));

                // Buscar el SubPortfolio
                SubPortfolio subPortfolio = subPortfolioRepository.findById(rule.getSubPortfolioId())
                        .orElseThrow(() -> new RuntimeException("Subcartera no encontrada: " + rule.getSubPortfolioId()));

                // Crear la nueva cabecera
                HeaderConfiguration newHeader = new HeaderConfiguration(
                        subPortfolio,
                        fieldDef,
                        rule.getTargetHeaderName(), // headerName (ej: "DNI")
                        rule.getTargetHeaderName(), // displayLabel (mismo que headerName)
                        null, // format
                        0, // required (no obligatorio por defecto)
                        LoadType.ACTUALIZACION // tipo de carga diaria
                );

                HeaderConfiguration savedHeader = headerConfigRepository.save(newHeader);

                System.out.println("✅ Regla de transformación creada: " +
                                 savedRule.getTargetField() + " desde " + savedRule.getSourceField());
                System.out.println("✅ Cabecera automática creada: " + savedHeader.getHeaderName() +
                                 " → " + fieldDef.getFieldCode());
            } catch (Exception headerEx) {
                System.err.println("⚠️ Error creando cabecera automática: " + headerEx.getMessage());
                // No fallar la creación de la regla si falla la cabecera
            }

            return ResponseEntity.ok(savedRule);
        } catch (Exception e) {
            System.err.println("❌ Error creando regla: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar regla de transformación",
               description = "Actualiza una regla existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Regla actualizada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Regla no encontrada")
    })
    @PutMapping("/{ruleId}")
    public ResponseEntity<?> updateRule(
            @Parameter(description = "ID de la regla") @PathVariable Long ruleId,
            @RequestBody FieldTransformationRule updatedRule) {

        return repository.findById(ruleId)
                .map(existingRule -> {
                    // Actualizar campos
                    existingRule.setTargetField(updatedRule.getTargetField());
                    existingRule.setSourceField(updatedRule.getSourceField());
                    existingRule.setTargetHeaderName(updatedRule.getTargetHeaderName());
                    existingRule.setSubPortfolioId(updatedRule.getSubPortfolioId());
                    existingRule.setStartsWithPrefix(updatedRule.getStartsWithPrefix());
                    existingRule.setExtractLastNChars(updatedRule.getExtractLastNChars());
                    existingRule.setRegexPattern(updatedRule.getRegexPattern());
                    existingRule.setRegexCaptureGroup(updatedRule.getRegexCaptureGroup());
                    existingRule.setRuleOrder(updatedRule.getRuleOrder());
                    existingRule.setIsActive(updatedRule.getIsActive());
                    existingRule.setDescription(updatedRule.getDescription());

                    FieldTransformationRule saved = repository.save(existingRule);
                    System.out.println("✅ Regla actualizada: ID=" + ruleId);

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar regla de transformación",
               description = "Elimina una regla existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Regla eliminada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Regla no encontrada")
    })
    @DeleteMapping("/{ruleId}")
    public ResponseEntity<?> deleteRule(
            @Parameter(description = "ID de la regla") @PathVariable Long ruleId) {

        return repository.findById(ruleId)
                .map(rule -> {
                    repository.delete(rule);
                    System.out.println("✅ Regla eliminada: ID=" + ruleId);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Regla eliminada exitosamente");

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Activar/Desactivar regla",
               description = "Cambia el estado activo/inactivo de una regla")
    @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente")
    @PatchMapping("/{ruleId}/toggle-active")
    public ResponseEntity<?> toggleRuleActive(
            @Parameter(description = "ID de la regla") @PathVariable Long ruleId) {

        return repository.findById(ruleId)
                .map(rule -> {
                    rule.setIsActive(!rule.getIsActive());
                    FieldTransformationRule saved = repository.save(rule);

                    System.out.println("✅ Regla " + (saved.getIsActive() ? "activada" : "desactivada") +
                                     ": ID=" + ruleId);

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
