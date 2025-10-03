package com.cashi.collectionmanagement.interfaces.rest.controllers;

import com.cashi.collectionmanagement.application.internal.commandservices.ManagementCommandServiceImpl;
import com.cashi.collectionmanagement.application.internal.queryservices.ManagementQueryServiceImpl;
import com.cashi.collectionmanagement.domain.model.queries.*;
import com.cashi.collectionmanagement.interfaces.rest.resources.*;
import com.cashi.collectionmanagement.interfaces.rest.transform.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/managements")
@Tag(name = "Collection Management", description = "Gestión de cobranzas, llamadas y compromisos de pago")
public class ManagementController {

    private final ManagementCommandServiceImpl commandService;
    private final ManagementQueryServiceImpl queryService;

    public ManagementController(ManagementCommandServiceImpl commandService, ManagementQueryServiceImpl queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @Operation(summary = "Crear nueva gestión de cobranza", description = "Registra una nueva gestión de cobranza con tipificación de contacto y gestión")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Gestión creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<ManagementResource> createManagement(@RequestBody CreateManagementRequest request) {
        var command = CreateManagementCommandFromResourceAssembler.toCommandFromResource(request);
        var management = commandService.handle(command);
        var resource = ManagementResourceFromEntityAssembler.toResourceFromEntity(management);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Operation(summary = "Obtener gestión por ID", description = "Retorna una gestión específica con todos sus detalles")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Gestión encontrada"),
        @ApiResponse(responseCode = "404", description = "Gestión no encontrada")
    })
    @GetMapping("/{managementId}")
    public ResponseEntity<ManagementResource> getManagementById(
            @Parameter(description = "ID de la gestión") @PathVariable String managementId) {
        var query = new GetManagementByIdQuery(managementId);
        return queryService.handle(query)
                .map(management -> ResponseEntity.ok(
                        ManagementResourceFromEntityAssembler.toResourceFromEntity(management)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{managementId}")
    public ResponseEntity<ManagementResource> updateManagement(
            @PathVariable String managementId,
            @RequestBody UpdateManagementRequest request) {
        var command = UpdateManagementCommandFromResourceAssembler.toCommandFromResource(managementId, request);
        var management = commandService.handle(command);
        var resource = ManagementResourceFromEntityAssembler.toResourceFromEntity(management);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ManagementResource>> getManagementsByCustomer(@PathVariable String customerId) {
        var query = new GetManagementsByCustomerQuery(customerId);
        var managements = queryService.handle(query);
        var resources = managements.stream()
                .map(ManagementResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/advisor/{advisorId}")
    public ResponseEntity<List<ManagementResource>> getManagementsByAdvisor(@PathVariable String advisorId) {
        var query = new GetManagementsByAdvisorQuery(advisorId);
        var managements = queryService.handle(query);
        var resources = managements.stream()
                .map(ManagementResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<ManagementResource>> getManagementsByCampaign(@PathVariable String campaignId) {
        var query = new GetManagementsByCampaignQuery(campaignId);
        var managements = queryService.handle(query);
        var resources = managements.stream()
                .map(ManagementResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<ManagementResource>> getManagementsByDateRange(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        var query = new GetManagementsByDateRangeQuery(startDate, endDate);
        var managements = queryService.handle(query);
        var resources = managements.stream()
                .map(ManagementResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Iniciar llamada", description = "Registra el inicio de una llamada en la gestión")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Llamada iniciada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Gestión no encontrada")
    })
    @PostMapping("/{managementId}/call/start")
    public ResponseEntity<ManagementResource> startCall(
            @Parameter(description = "ID de la gestión") @PathVariable String managementId,
            @RequestBody StartCallRequest request) {
        var command = StartCallCommandFromResourceAssembler.toCommandFromResource(managementId, request);
        var management = commandService.handle(command);
        var resource = ManagementResourceFromEntityAssembler.toResourceFromEntity(management);
        return ResponseEntity.ok(resource);
    }

    @Operation(summary = "Finalizar llamada", description = "Registra el fin de una llamada y calcula la duración")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Llamada finalizada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Gestión no encontrada")
    })
    @PostMapping("/{managementId}/call/end")
    public ResponseEntity<ManagementResource> endCall(
            @Parameter(description = "ID de la gestión") @PathVariable String managementId,
            @RequestBody EndCallRequest request) {
        var command = EndCallCommandFromResourceAssembler.toCommandFromResource(managementId, request);
        var management = commandService.handle(command);
        var resource = ManagementResourceFromEntityAssembler.toResourceFromEntity(management);
        return ResponseEntity.ok(resource);
    }

    @Operation(summary = "Registrar compromiso de pago", description = "Registra un compromiso o pago asociado a la gestión")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pago registrado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Gestión no encontrada")
    })
    @PostMapping("/{managementId}/payment")
    public ResponseEntity<ManagementResource> registerPayment(
            @Parameter(description = "ID de la gestión") @PathVariable String managementId,
            @RequestBody RegisterPaymentRequest request) {
        var command = RegisterPaymentCommandFromResourceAssembler.toCommandFromResource(managementId, request);
        var management = commandService.handle(command);
        var resource = ManagementResourceFromEntityAssembler.toResourceFromEntity(management);
        return ResponseEntity.ok(resource);
    }
}
