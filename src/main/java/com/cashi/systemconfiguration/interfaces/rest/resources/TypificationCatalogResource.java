package com.cashi.systemconfiguration.interfaces.rest.resources;

public record TypificationCatalogResource(
    Integer id,
    String code,
    String name,
    String classificationType,
    Integer parentTypificationId,
    Integer hierarchyLevel,
    String hierarchyPath,
    String description,
    Integer displayOrder,
    String iconName,
    String colorHex,
    Integer isSystem,
    Integer isActive,
    // Campos del tipo de clasificación
    Boolean suggestsFullAmount,
    Boolean allowsInstallmentSelection,
    Boolean requiresManualAmount
) {}
