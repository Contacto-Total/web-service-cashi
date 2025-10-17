package com.cashi.customermanagement.interfaces.rest.controllers;

import com.cashi.customermanagement.domain.model.commands.SaveCustomerOutputConfigCommand;
import com.cashi.customermanagement.domain.model.queries.GetCustomerOutputConfigQuery;
import com.cashi.customermanagement.domain.services.CustomerOutputConfigCommandService;
import com.cashi.customermanagement.domain.services.CustomerOutputConfigQueryService;
import com.cashi.customermanagement.interfaces.rest.resources.CustomerOutputConfigResource;
import com.cashi.customermanagement.interfaces.rest.resources.SaveCustomerOutputConfigRequest;
import com.cashi.customermanagement.interfaces.rest.transform.CustomerOutputConfigResourceFromEntityAssembler;
import com.cashi.customermanagement.interfaces.rest.transform.SaveCustomerOutputConfigCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller para configuración de outputs del cliente
 *
 * ENDPOINTS:
 * - POST /api/v1/customer-outputs/config → Guardar/actualizar configuración
 * - GET /api/v1/customer-outputs/config → Obtener configuración
 *
 * FLUJO COMPLETO:
 * 1. Administrador configura outputs en: /maintenance/customer-outputs
 * 2. Frontend llama: POST /api/v1/customer-outputs/config
 * 3. Backend guarda en tabla: customer_output_config
 * 4. Pantalla de gestión llama: GET /api/v1/customer-outputs/config?tenantId=X&portfolioId=Y
 * 5. Frontend muestra campos según configuración
 */
@Tag(name = "Customer Output Configuration", description = "Configuración de outputs del cliente para pantalla de gestión")
@RestController
@RequestMapping("/api/v1/customer-outputs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CustomerOutputConfigController {

    private final CustomerOutputConfigCommandService commandService;
    private final CustomerOutputConfigQueryService queryService;

    /**
     * Guarda o actualiza configuración de outputs del cliente
     *
     * LÓGICA:
     * - Si existe config para tenant+portfolio → actualiza
     * - Si no existe → crea nueva
     *
     * EJEMPLO REQUEST:
     * POST /api/v1/customer-outputs/config
     * {
     *   "tenantId": 2,
     *   "portfolioId": 5,
     *   "fieldsConfig": "[{\"id\":\"documentCode\",\"label\":\"DNI/Documento\",\"field\":\"documentCode\",\"category\":\"personal\",\"format\":\"text\",\"isVisible\":true,\"displayOrder\":1,\"highlight\":false}]"
     * }
     */
    @Operation(summary = "Guardar configuración de outputs",
               description = "Guarda o actualiza la configuración de qué campos del cliente mostrar en gestión")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuración guardada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Request inválido"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/config")
    public ResponseEntity<CustomerOutputConfigResource> saveConfiguration(
            @RequestBody SaveCustomerOutputConfigRequest request) {

        System.out.println("📤 POST /api/v1/customer-outputs/config");
        System.out.println("   - tenantId: " + request.tenantId());
        System.out.println("   - portfolioId: " + (request.portfolioId() != null ? request.portfolioId() : "null (general)"));
        System.out.println("   - fieldsConfig length: " + request.fieldsConfig().length() + " chars");

        try {
            // Convertir Request → Command
            SaveCustomerOutputConfigCommand command =
                SaveCustomerOutputConfigCommandFromResourceAssembler.toCommandFromResource(request);

            // Ejecutar comando
            var savedConfig = commandService.handle(command);

            // Convertir Entity → Resource
            CustomerOutputConfigResource resource =
                CustomerOutputConfigResourceFromEntityAssembler.toResourceFromEntity(savedConfig);

            System.out.println("   ✅ Configuración guardada (ID: " + resource.id() + ")");
            return ResponseEntity.ok(resource);

        } catch (Exception e) {
            System.err.println("   ❌ Error guardando configuración: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene configuración de outputs del cliente
     *
     * LÓGICA DE BÚSQUEDA:
     * 1. Si portfolioId != null → busca configuración específica del portfolio
     * 2. Si no encuentra específica → busca configuración general del tenant
     * 3. Si no encuentra ninguna → 200 OK con id=null (frontend usa valores por defecto)
     *
     * RESPUESTAS:
     * - 200 OK con id=null: No hay configuración guardada → Frontend usa campos por defecto
     * - 200 OK con id=X, fieldsConfig="[]": Hay config guardada pero vacía → No mostrar campos
     * - 200 OK con id=X, fieldsConfig="[...]": Hay config con campos → Mostrar esos campos
     *
     * EJEMPLO REQUEST:
     * GET /api/v1/customer-outputs/config?tenantId=2&portfolioId=5
     * GET /api/v1/customer-outputs/config?tenantId=2  (sin portfolio = buscar general)
     */
    @Operation(summary = "Obtener configuración de outputs",
               description = "Obtiene la configuración de campos del cliente. Busca específica por portfolio o general del tenant.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Siempre retorna 200 OK. Si id=null, no hay configuración guardada."),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    @GetMapping("/config")
    public ResponseEntity<CustomerOutputConfigResource> getConfiguration(
            @Parameter(description = "ID del tenant (financiera)", required = true)
            @RequestParam Long tenantId,
            @Parameter(description = "ID del portfolio (cartera). Opcional - si no se envía, busca configuración general")
            @RequestParam(required = false) Long portfolioId) {

        System.out.println("📥 GET /api/v1/customer-outputs/config");
        System.out.println("   - tenantId: " + tenantId);
        System.out.println("   - portfolioId: " + (portfolioId != null ? portfolioId : "null (buscar general)"));

        // Validar parámetros
        if (tenantId == null || tenantId <= 0) {
            System.err.println("   ❌ tenantId inválido");
            return ResponseEntity.badRequest().build();
        }

        try {
            // Crear query
            GetCustomerOutputConfigQuery query = new GetCustomerOutputConfigQuery(tenantId, portfolioId);

            // Ejecutar query
            var config = queryService.handle(query);

            if (config.isPresent()) {
                // Configuración encontrada en BD
                CustomerOutputConfigResource resource =
                    CustomerOutputConfigResourceFromEntityAssembler.toResourceFromEntity(config.get());

                System.out.println("   ✅ Configuración encontrada (ID: " + resource.id() + ")");
                return ResponseEntity.ok(resource);
            } else {
                // No hay configuración guardada - retornar 200 OK con id=null
                System.out.println("   ⚠️ No hay configuración guardada. Retornando config vacía con id=null.");
                CustomerOutputConfigResource emptyResource = new CustomerOutputConfigResource(
                    null,           // id = null indica que NO existe en BD
                    tenantId,       // tenantId solicitado
                    portfolioId,    // portfolioId solicitado (puede ser null)
                    "[]"            // array vacío de campos
                );
                return ResponseEntity.ok(emptyResource);
            }

        } catch (Exception e) {
            System.err.println("   ❌ Error obteniendo configuración: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
