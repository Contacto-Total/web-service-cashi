package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.systemconfiguration.application.internal.queryservices.SystemConfigQueryServiceImpl;
import com.cashi.systemconfiguration.interfaces.rest.resources.CampaignResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.ContactClassificationResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.ManagementClassificationResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.PortfolioResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.TenantResource;
import com.cashi.systemconfiguration.interfaces.rest.transform.CampaignResourceFromEntityAssembler;
import com.cashi.systemconfiguration.interfaces.rest.transform.ContactClassificationResourceFromEntityAssembler;
import com.cashi.systemconfiguration.interfaces.rest.transform.ManagementClassificationResourceFromEntityAssembler;
import com.cashi.systemconfiguration.interfaces.rest.transform.PortfolioResourceFromEntityAssembler;
import com.cashi.systemconfiguration.interfaces.rest.transform.TenantResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system-config")
@Tag(name = "System Configuration", description = "Gestión de configuración del sistema, campañas y tipificaciones")
public class SystemConfigController {

    private final SystemConfigQueryServiceImpl queryService;

    public SystemConfigController(SystemConfigQueryServiceImpl queryService) {
        this.queryService = queryService;
    }

    @Operation(
        summary = "Obtener tipificaciones de contacto",
        description = "Retorna todas las tipificaciones de contacto disponibles (CPC, NCL, BZN, etc.)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de tipificaciones obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = ContactClassificationResource.class)))
    })
    @GetMapping("/contact-classifications")
    public ResponseEntity<List<ContactClassificationResource>> getAllContactClassifications() {
        var classifications = queryService.getAllContactClassifications();
        var resources = classifications.stream()
                .map(ContactClassificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(
        summary = "Obtener tipificaciones de gestión",
        description = "Retorna todas las tipificaciones de gestión disponibles (ACP, PGR, CNV, etc.)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de tipificaciones obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = ManagementClassificationResource.class)))
    })
    @GetMapping("/management-classifications")
    public ResponseEntity<List<ManagementClassificationResource>> getAllManagementClassifications() {
        var classifications = queryService.getAllManagementClassifications();
        var resources = classifications.stream()
                .map(ManagementClassificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/management-classifications/requires-payment")
    public ResponseEntity<List<ManagementClassificationResource>> getManagementClassificationsByPayment(
            @RequestParam(defaultValue = "true") Boolean requiresPayment) {
        var classifications = queryService.getManagementClassificationsByPaymentRequirement(requiresPayment);
        var resources = classifications.stream()
                .map(ManagementClassificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/management-classifications/requires-schedule")
    public ResponseEntity<List<ManagementClassificationResource>> getManagementClassificationsBySchedule(
            @RequestParam(defaultValue = "true") Boolean requiresSchedule) {
        var classifications = queryService.getManagementClassificationsByScheduleRequirement(requiresSchedule);
        var resources = classifications.stream()
                .map(ManagementClassificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Obtener todas las campañas", description = "Retorna lista completa de campañas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de campañas obtenida exitosamente")
    })
    @GetMapping("/campaigns")
    public ResponseEntity<List<CampaignResource>> getAllCampaigns() {
        var campaigns = queryService.getAllCampaigns();
        var resources = campaigns.stream()
                .map(CampaignResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Obtener campañas activas", description = "Retorna solo las campañas activas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de campañas activas obtenida exitosamente")
    })
    @GetMapping("/campaigns/active")
    public ResponseEntity<List<CampaignResource>> getActiveCampaigns() {
        var campaigns = queryService.getActiveCampaigns();
        var resources = campaigns.stream()
                .map(CampaignResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Obtener campaña por ID", description = "Retorna una campaña específica por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Campaña encontrada"),
        @ApiResponse(responseCode = "404", description = "Campaña no encontrada")
    })
    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<CampaignResource> getCampaignById(
            @Parameter(description = "ID de la campaña", example = "CAMP-001")
            @PathVariable String campaignId) {
        return queryService.getCampaignById(campaignId)
                .map(campaign -> ResponseEntity.ok(CampaignResourceFromEntityAssembler.toResourceFromEntity(campaign)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener todos los tenants activos", description = "Retorna la lista de todos los clientes/tenants activos en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de tenants obtenida exitosamente")
    })
    @GetMapping("/tenants")
    public ResponseEntity<List<TenantResource>> getAllTenants() {
        var tenants = queryService.getAllActiveTenants();
        var resources = tenants.stream()
                .map(TenantResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Obtener subcarteras por tenant", description = "Retorna todas las subcarteras de un tenant ordenadas jerárquicamente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de subcarteras obtenida exitosamente")
    })
    @GetMapping("/tenants/{tenantId}/portfolios")
    public ResponseEntity<List<PortfolioResource>> getPortfoliosByTenant(
            @Parameter(description = "ID del tenant", example = "1")
            @PathVariable Long tenantId) {
        var portfolios = queryService.getPortfoliosByTenantId(tenantId);
        var resources = portfolios.stream()
                .map(PortfolioResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
