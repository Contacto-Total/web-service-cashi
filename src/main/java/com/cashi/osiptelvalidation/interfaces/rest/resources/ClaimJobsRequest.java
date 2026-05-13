package com.cashi.osiptelvalidation.interfaces.rest.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ClaimJobsRequest(
        @NotBlank String workerId,
        @Min(1) @Max(20) int limit
) {}
