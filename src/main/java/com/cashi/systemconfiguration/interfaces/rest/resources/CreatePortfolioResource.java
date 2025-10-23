package com.cashi.systemconfiguration.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para crear un nuevo portfolio/cartera")
public record CreatePortfolioResource(
    @Schema(description = "ID del tenant/cliente al que pertenece", example = "1", required = true)
    Integer tenantId,

    @Schema(description = "Código único del portfolio", example = "TRAMO-1", required = true)
    String portfolioCode,

    @Schema(description = "Nombre del portfolio", example = "Tramo 1", required = true)
    String portfolioName,

    @Schema(description = "Descripción del portfolio", example = "Cartera de cobranza - Tramo 1 (1-30 días de mora)")
    String description
) {}
