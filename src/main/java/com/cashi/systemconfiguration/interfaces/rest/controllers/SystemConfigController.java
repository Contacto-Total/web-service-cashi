package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.systemconfiguration.application.internal.commandservices.PortfolioCommandServiceImpl;
import com.cashi.systemconfiguration.interfaces.rest.resources.CreatePortfolioResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.PortfolioResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.TenantResource;
import com.cashi.systemconfiguration.interfaces.rest.transform.PortfolioResourceFromEntityAssembler;
import com.cashi.systemconfiguration.interfaces.rest.transform.TenantResourceFromEntityAssembler;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.TenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system-config")
@Tag(name = "System Configuration", description = "Gestión de configuración del sistema, tenants y portafolios")
public class SystemConfigController {

    private final TenantRepository tenantRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioCommandServiceImpl portfolioCommandService;

    public SystemConfigController(
            TenantRepository tenantRepository,
            PortfolioRepository portfolioRepository,
            PortfolioCommandServiceImpl portfolioCommandService) {
        this.tenantRepository = tenantRepository;
        this.portfolioRepository = portfolioRepository;
        this.portfolioCommandService = portfolioCommandService;
    }

    @Operation(summary = "Obtener todos los tenants activos", description = "Retorna la lista de todos los clientes/tenants activos en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de tenants obtenida exitosamente")
    })
    @GetMapping("/tenants")
    public ResponseEntity<List<TenantResource>> getAllTenants() {
        var tenants = tenantRepository.findAll().stream()
            .filter(t -> t.getIsActive())
            .toList();
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
        var tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        var portfolios = portfolioRepository.findByTenantOrderedByHierarchy(tenantOpt.get());
        var resources = portfolios.stream()
                .map(PortfolioResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Crear nuevo portfolio/cartera", description = "Crea un nuevo portfolio o subcartera para un tenant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Portfolio creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o portfolio duplicado"),
        @ApiResponse(responseCode = "404", description = "Tenant o portfolio padre no encontrado")
    })
    @PostMapping("/portfolios")
    public ResponseEntity<PortfolioResource> createPortfolio(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del portfolio a crear")
            @RequestBody CreatePortfolioResource resource) {
        try {
            var portfolio = portfolioCommandService.createPortfolio(
                resource.tenantId(),
                resource.portfolioCode(),
                resource.portfolioName(),
                resource.portfolioType(),
                resource.parentPortfolioId(),
                resource.description()
            );
            var portfolioResource = PortfolioResourceFromEntityAssembler.toResourceFromEntity(portfolio);
            return ResponseEntity.status(HttpStatus.CREATED).body(portfolioResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
