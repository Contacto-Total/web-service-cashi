package com.cashi.systemconfiguration.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTypificationCommand(
    @NotBlank(message = "Code is required")
    @Size(max = 20, message = "Code must be at most 20 characters")
    String code,

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,

    @NotNull(message = "Classification type is required")
    String classificationType,

    Integer parentTypificationId,

    String description,

    Integer displayOrder,

    String iconName,

    String colorHex,

    String metadataSchema
) {}
