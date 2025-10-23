package com.cashi.systemconfiguration.interfaces.rest;

import com.cashi.systemconfiguration.domain.services.SubPortfolioCommandService;
import com.cashi.systemconfiguration.domain.services.SubPortfolioQueryService;
import com.cashi.systemconfiguration.interfaces.rest.resources.CreateSubPortfolioResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.SubPortfolioResource;
import com.cashi.systemconfiguration.interfaces.rest.resources.UpdateSubPortfolioResource;
import com.cashi.systemconfiguration.interfaces.rest.transform.SubPortfolioResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subportfolios")
@Tag(name = "SubPortfolios", description = "API de gestión de subcarteras")
public class SubPortfolioController {

    private final SubPortfolioCommandService subPortfolioCommandService;
    private final SubPortfolioQueryService subPortfolioQueryService;

    public SubPortfolioController(SubPortfolioCommandService subPortfolioCommandService,
                                 SubPortfolioQueryService subPortfolioQueryService) {
        this.subPortfolioCommandService = subPortfolioCommandService;
        this.subPortfolioQueryService = subPortfolioQueryService;
    }

    @PostMapping
    @Operation(summary = "Crear una nueva subcartera")
    public ResponseEntity<SubPortfolioResource> createSubPortfolio(@RequestBody CreateSubPortfolioResource resource) {
        var subPortfolio = subPortfolioCommandService.createSubPortfolio(
            resource.portfolioId(),
            resource.subPortfolioCode(),
            resource.subPortfolioName(),
            resource.description()
        );

        var subPortfolioResource = SubPortfolioResourceFromEntityAssembler.toResourceFromEntity(subPortfolio);
        return new ResponseEntity<>(subPortfolioResource, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Obtener todas las subcarteras")
    public ResponseEntity<List<SubPortfolioResource>> getAllSubPortfolios() {
        var subPortfolios = subPortfolioQueryService.getAllSubPortfolios();

        var resources = subPortfolios.stream()
            .map(SubPortfolioResourceFromEntityAssembler::toResourceFromEntity)
            .toList();

        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{subPortfolioId}")
    @Operation(summary = "Obtener una subcartera por ID")
    public ResponseEntity<SubPortfolioResource> getSubPortfolioById(@PathVariable Integer subPortfolioId) {
        var subPortfolio = subPortfolioQueryService.getSubPortfolioById(subPortfolioId);

        if (subPortfolio.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var resource = SubPortfolioResourceFromEntityAssembler.toResourceFromEntity(subPortfolio.get());
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/by-portfolio/{portfolioId}")
    @Operation(summary = "Obtener todas las subcarteras de un portfolio")
    public ResponseEntity<List<SubPortfolioResource>> getSubPortfoliosByPortfolio(@PathVariable Integer portfolioId) {
        var subPortfolios = subPortfolioQueryService.getSubPortfoliosByPortfolio(portfolioId);

        var resources = subPortfolios.stream()
            .map(SubPortfolioResourceFromEntityAssembler::toResourceFromEntity)
            .toList();

        return ResponseEntity.ok(resources);
    }

    @GetMapping("/by-portfolio/{portfolioId}/active")
    @Operation(summary = "Obtener todas las subcarteras activas de un portfolio")
    public ResponseEntity<List<SubPortfolioResource>> getActiveSubPortfoliosByPortfolio(@PathVariable Integer portfolioId) {
        var subPortfolios = subPortfolioQueryService.getActiveSubPortfoliosByPortfolio(portfolioId);

        var resources = subPortfolios.stream()
            .map(SubPortfolioResourceFromEntityAssembler::toResourceFromEntity)
            .toList();

        return ResponseEntity.ok(resources);
    }

    @GetMapping("/by-tenant/{tenantId}")
    @Operation(summary = "Obtener todas las subcarteras de un tenant")
    public ResponseEntity<List<SubPortfolioResource>> getSubPortfoliosByTenant(@PathVariable Integer tenantId) {
        var subPortfolios = subPortfolioQueryService.getSubPortfoliosByTenant(tenantId);

        var resources = subPortfolios.stream()
            .map(SubPortfolioResourceFromEntityAssembler::toResourceFromEntity)
            .toList();

        return ResponseEntity.ok(resources);
    }

    @PutMapping("/{subPortfolioId}")
    @Operation(summary = "Actualizar una subcartera")
    public ResponseEntity<SubPortfolioResource> updateSubPortfolio(
        @PathVariable Integer subPortfolioId,
        @RequestBody UpdateSubPortfolioResource resource
    ) {
        var subPortfolio = subPortfolioCommandService.updateSubPortfolio(
            subPortfolioId,
            resource.subPortfolioName(),
            resource.description()
        );

        var subPortfolioResource = SubPortfolioResourceFromEntityAssembler.toResourceFromEntity(subPortfolio);
        return ResponseEntity.ok(subPortfolioResource);
    }

    @PatchMapping("/{subPortfolioId}/toggle-status")
    @Operation(summary = "Activar o desactivar una subcartera")
    public ResponseEntity<SubPortfolioResource> toggleSubPortfolioStatus(
        @PathVariable Integer subPortfolioId,
        @RequestParam Boolean isActive
    ) {
        var subPortfolio = subPortfolioCommandService.toggleSubPortfolioStatus(subPortfolioId, isActive ? 1 : 0);
        var resource = SubPortfolioResourceFromEntityAssembler.toResourceFromEntity(subPortfolio);
        return ResponseEntity.ok(resource);
    }

    @DeleteMapping("/{subPortfolioId}")
    @Operation(summary = "Eliminar una subcartera (soft delete)")
    public ResponseEntity<Void> deleteSubPortfolio(@PathVariable Integer subPortfolioId) {
        subPortfolioCommandService.deleteSubPortfolio(subPortfolioId);
        return ResponseEntity.noContent().build();
    }
}
