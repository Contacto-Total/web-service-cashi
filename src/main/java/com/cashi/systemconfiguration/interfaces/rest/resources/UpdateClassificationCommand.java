package com.cashi.systemconfiguration.interfaces.rest.resources;

public record UpdateClassificationCommand(
    String name,
    String description,
    Integer displayOrder,
    String iconName,
    String colorHex,
    Boolean isActive,
    String metadataSchema
) {}
