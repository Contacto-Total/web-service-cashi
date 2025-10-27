package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.systemconfiguration.domain.model.entities.TypificationCatalog;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.TypificationCatalogRepository;
import com.cashi.systemconfiguration.interfaces.rest.resources.TypificationCatalogResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Typification Catalog operations
 */
@RestController
@RequestMapping("/api/v1/typifications")
public class TypificationController {

    private final TypificationCatalogRepository typificationCatalogRepository;

    public TypificationController(TypificationCatalogRepository typificationCatalogRepository) {
        this.typificationCatalogRepository = typificationCatalogRepository;
    }

    /**
     * Get all active typifications
     * GET /api/v1/typifications
     */
    @GetMapping
    public ResponseEntity<List<TypificationCatalogResource>> getAllTypifications() {
        List<TypificationCatalog> typifications = typificationCatalogRepository.findAllActive();

        List<TypificationCatalogResource> resources = typifications.stream()
            .map(this::toResource)
            .collect(Collectors.toList());

        return ResponseEntity.ok(resources);
    }

    /**
     * Get typifications by classification type
     * GET /api/v1/typifications/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<TypificationCatalogResource>> getTypificationsByType(
            @PathVariable String type) {
        try {
            TypificationCatalog.ClassificationType classificationType =
                TypificationCatalog.ClassificationType.valueOf(type.toUpperCase());

            List<TypificationCatalog> typifications = typificationCatalogRepository
                .findActiveByType(classificationType);

            List<TypificationCatalogResource> resources = typifications.stream()
                .map(this::toResource)
                .collect(Collectors.toList());

            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get typifications by hierarchy level
     * GET /api/v1/typifications/level/{level}
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<List<TypificationCatalogResource>> getTypificationsByLevel(
            @PathVariable Integer level) {
        List<TypificationCatalog> typifications = typificationCatalogRepository
            .findByHierarchyLevel(level);

        List<TypificationCatalogResource> resources = typifications.stream()
            .map(this::toResource)
            .collect(Collectors.toList());

        return ResponseEntity.ok(resources);
    }

    /**
     * Get child typifications by parent ID
     * GET /api/v1/typifications/{parentId}/children
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<TypificationCatalogResource>> getChildTypifications(
            @PathVariable Integer parentId) {
        TypificationCatalog parent = typificationCatalogRepository.findById(parentId)
            .orElse(null);

        if (parent == null) {
            return ResponseEntity.notFound().build();
        }

        List<TypificationCatalog> children = typificationCatalogRepository
            .findByParentId(parentId);

        List<TypificationCatalogResource> resources = children.stream()
            .map(this::toResource)
            .collect(Collectors.toList());

        return ResponseEntity.ok(resources);
    }

    /**
     * Get typification by ID
     * GET /api/v1/typifications/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TypificationCatalogResource> getTypificationById(
            @PathVariable Integer id) {
        return typificationCatalogRepository.findById(id)
            .filter(t -> t.getIsActive() == 1 && t.getDeletedAt() == null)
            .map(this::toResource)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Convert entity to resource
     */
    private TypificationCatalogResource toResource(TypificationCatalog typification) {
        return new TypificationCatalogResource(
            typification.getId(),
            typification.getCode(),
            typification.getName(),
            typification.getClassificationType().name(),
            typification.getParentTypification() != null ?
                typification.getParentTypification().getId() : null,
            typification.getHierarchyLevel(),
            typification.getHierarchyPath(),
            typification.getDescription(),
            typification.getDisplayOrder(),
            typification.getIconName(),
            typification.getColorHex(),
            typification.getIsSystem(),
            typification.getIsActive(),
            null,  // suggestsFullAmount - TODO: get from metadata
            null,  // allowsInstallmentSelection - TODO: get from metadata
            null   // requiresManualAmount - TODO: get from metadata
        );
    }
}
