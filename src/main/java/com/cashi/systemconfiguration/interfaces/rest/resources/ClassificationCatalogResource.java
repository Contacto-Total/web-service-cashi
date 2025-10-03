package com.cashi.systemconfiguration.interfaces.rest.resources;

public record ClassificationCatalogResource(
    Long id,
    String code,
    String name,
    String classificationType,
    Long parentClassificationId,
    Integer hierarchyLevel,
    String hierarchyPath,
    String description,
    Integer displayOrder,
    String iconName,
    String colorHex,
    Boolean isSystem,
    Boolean isActive
) {}
