package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.systemconfiguration.application.internal.commandservices.PortfolioCommandServiceImpl;
import com.cashi.systemconfiguration.application.internal.commandservices.TenantCommandServiceImpl;
import com.cashi.systemconfiguration.domain.model.commands.CreateTenantCommand;
import com.cashi.systemconfiguration.domain.model.commands.UpdatePortfolioCommand;
import com.cashi.systemconfiguration.domain.model.commands.UpdateTenantCommand;
import com.cashi.systemconfiguration.interfaces.rest.resources.CreatePortfolioResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.CreateTenantResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.PortfolioResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.TenantResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.UpdatePortfolioResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.UpdateTenantResource;
import com.cashi.systemconfiguration.interfaces.rest.transform.PortfolioResourceFromEntityAssembler;
import com.cashi.systemconfiguration.interfaces.rest.transform.TenantResourceFromEntityAssembler;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
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
    private final SubPortfolioRepository subPortfolioRepository;
    private final PortfolioCommandServiceImpl portfolioCommandService;
    private final TenantCommandServiceImpl tenantCommandService;

    public SystemConfigController(
            TenantRepository tenantRepository,
            PortfolioRepository portfolioRepository,
            SubPortfolioRepository subPortfolioRepository,
            PortfolioCommandServiceImpl portfolioCommandService,
            TenantCommandServiceImpl tenantCommandService) {
        this.tenantRepository = tenantRepository;
        this.portfolioRepository = portfolioRepository;
        this.subPortfolioRepository = subPortfolioRepository;
        this.portfolioCommandService = portfolioCommandService;
        this.tenantCommandService = tenantCommandService;
    }

    @Operation(summary = "Obtener todos los tenants activos", description = "Retorna la lista de todos los clientes/tenants activos en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de tenants obtenida exitosamente")
    })
    @GetMapping("/tenants")
    public ResponseEntity<List<TenantResource>> getAllTenants() {
        var tenants = tenantRepository.findAll();
        var resources = tenants.stream()
                .map(tenant -> {
                    boolean hasPortfolios = portfolioRepository.countByTenant(tenant) > 0;
                    return TenantResourceFromEntityAssembler.toResourceFromEntity(tenant, hasPortfolios);
                })
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
            @PathVariable Integer tenantId) {
        var tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        var portfolios = portfolioRepository.findByTenant(tenantOpt.get());
        var resources = portfolios.stream()
                .map(portfolio -> {
                    boolean hasSubPortfolios = subPortfolioRepository.countByPortfolio(portfolio) > 0;
                    return PortfolioResourceFromEntityAssembler.toResourceFromEntity(portfolio, hasSubPortfolios);
                })
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
                resource.description()
            );
            var portfolioResource = PortfolioResourceFromEntityAssembler.toResourceFromEntity(portfolio);
            return ResponseEntity.status(HttpStatus.CREATED).body(portfolioResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Crear nuevo tenant", description = "Crea un nuevo cliente/tenant en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tenant creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o código de tenant duplicado")
    })
    @PostMapping("/tenants")
    public ResponseEntity<TenantResource> createTenant(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del tenant a crear")
            @RequestBody CreateTenantResource resource) {
        try {
            var command = new CreateTenantCommand(
                resource.tenantCode(),
                resource.tenantName(),
                resource.businessName(),
                resource.taxId(),
                resource.countryCode(),
                resource.timezone(),
                resource.maxUsers(),
                resource.maxConcurrentSessions(),
                resource.subscriptionTier()
            );
            var tenant = tenantCommandService.handle(command);
            var tenantResource = TenantResourceFromEntityAssembler.toResourceFromEntity(tenant);
            return ResponseEntity.status(HttpStatus.CREATED).body(tenantResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Actualizar tenant", description = "Actualiza la información de un tenant existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tenant actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Tenant no encontrado")
    })
    @PutMapping("/tenants/{id}")
    public ResponseEntity<TenantResource> updateTenant(
            @Parameter(description = "ID del tenant", example = "1")
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del tenant a actualizar")
            @RequestBody UpdateTenantResource resource) {
        try {
            var command = new UpdateTenantCommand(
                resource.tenantName(),
                resource.businessName(),
                resource.isActive()
            );
            var tenant = tenantCommandService.handle(id, command);
            var tenantResource = TenantResourceFromEntityAssembler.toResourceFromEntity(tenant);
            return ResponseEntity.ok(tenantResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Eliminar tenant", description = "Desactiva un tenant (soft delete)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tenant desactivado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Tenant no encontrado")
    })
    @DeleteMapping("/tenants/{id}")
    public ResponseEntity<Void> deleteTenant(
            @Parameter(description = "ID del tenant", example = "1")
            @PathVariable Integer id) {
        try {
            tenantCommandService.deleteTenant(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Actualizar portfolio/cartera", description = "Actualiza la información de un portfolio existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Portfolio actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Portfolio no encontrado")
    })
    @PutMapping("/portfolios/{id}")
    public ResponseEntity<PortfolioResource> updatePortfolio(
            @Parameter(description = "ID del portfolio", example = "1")
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del portfolio a actualizar")
            @RequestBody UpdatePortfolioResource resource) {
        try {
            var command = new UpdatePortfolioCommand(
                resource.portfolioName(),
                resource.description(),
                resource.isActive()
            );
            var portfolio = portfolioCommandService.updatePortfolio(id, command);
            var portfolioResource = PortfolioResourceFromEntityAssembler.toResourceFromEntity(portfolio);
            return ResponseEntity.ok(portfolioResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Eliminar portfolio/cartera", description = "Desactiva un portfolio (soft delete)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Portfolio desactivado exitosamente"),
        @ApiResponse(responseCode = "400", description = "El portfolio tiene sub-portfolios activos"),
        @ApiResponse(responseCode = "404", description = "Portfolio no encontrado")
    })
    @DeleteMapping("/portfolios/{id}")
    public ResponseEntity<Void> deletePortfolio(
            @Parameter(description = "ID del portfolio", example = "1")
            @PathVariable Integer id) {
        try {
            portfolioCommandService.deletePortfolio(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
