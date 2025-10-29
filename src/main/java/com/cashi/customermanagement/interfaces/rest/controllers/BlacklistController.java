package com.cashi.customermanagement.interfaces.rest.controllers;

import com.cashi.customermanagement.domain.model.aggregates.Blacklist;
import com.cashi.customermanagement.domain.services.BlacklistCommandService;
import com.cashi.customermanagement.domain.services.BlacklistQueryService;
import com.cashi.customermanagement.interfaces.rest.resources.BlacklistResource;
import com.cashi.customermanagement.interfaces.rest.transform.BlacklistResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blacklist")
@Tag(name = "Blacklist", description = "Endpoints para gestión de blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistQueryService queryService;
    private final BlacklistCommandService commandService;
    private final BlacklistResourceFromEntityAssembler assembler;

    @GetMapping
    @Operation(summary = "Obtener todas las entradas de blacklist")
    public ResponseEntity<List<BlacklistResource>> getAllBlacklists() {
        var blacklists = queryService.getAllBlacklists();
        var resources = blacklists.stream()
                .map(assembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener blacklist por ID")
    public ResponseEntity<BlacklistResource> getBlacklistById(@PathVariable Long id) {
        return queryService.getBlacklistById(id)
                .map(assembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Obtener blacklists por proveedor")
    public ResponseEntity<List<BlacklistResource>> getBlacklistsByTenant(@PathVariable Long tenantId) {
        var blacklists = queryService.getBlacklistsByTenant(tenantId);
        var resources = blacklists.stream()
                .map(assembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @PostMapping
    @Operation(summary = "Crear nueva entrada en blacklist")
    public ResponseEntity<BlacklistResource> createBlacklist(@Valid @RequestBody BlacklistResource resource) {
        var blacklist = new Blacklist(
                resource.customerId(),
                resource.tenantId(),
                resource.tenantName(),
                resource.portfolioId(),
                resource.portfolioName(),
                resource.subPortfolioId(),
                resource.subPortfolioName(),
                resource.document(),
                resource.email(),
                resource.phone(),
                resource.startDate(),
                resource.endDate()
        );
        var created = commandService.createBlacklist(blacklist);
        return new ResponseEntity<>(assembler.toResourceFromEntity(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar entrada en blacklist")
    public ResponseEntity<BlacklistResource> updateBlacklist(@PathVariable Long id, @RequestBody BlacklistResource resource) {
        var blacklist = new Blacklist(
                resource.customerId(),
                resource.tenantId(),
                resource.tenantName(),
                resource.portfolioId(),
                resource.portfolioName(),
                resource.subPortfolioId(),
                resource.subPortfolioName(),
                resource.document(),
                resource.email(),
                resource.phone(),
                resource.startDate(),
                resource.endDate()
        );
        var updated = commandService.updateBlacklist(id, blacklist);
        return ResponseEntity.ok(assembler.toResourceFromEntity(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar entrada de blacklist")
    public ResponseEntity<Void> deleteBlacklist(@PathVariable Long id) {
        commandService.deleteBlacklist(id);
        return ResponseEntity.noContent().build();
    }
}
