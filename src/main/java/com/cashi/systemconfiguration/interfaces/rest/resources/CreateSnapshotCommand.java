package com.cashi.systemconfiguration.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record CreateSnapshotCommand(
    @NotBlank(message = "Version name is required")
    String versionName,

    String description
) {}
