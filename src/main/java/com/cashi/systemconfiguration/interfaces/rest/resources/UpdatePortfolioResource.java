package com.cashi.systemconfiguration.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para actualizar un portfolio/cartera existente")
public record UpdatePortfolioResource(
    @Schema(description = "Nombre del portfolio", example = "Tramo 1 Actualizado")
    String portfolioName,

    @Schema(description = "Descripción del portfolio", example = "Cartera actualizada de cobranza")
    String description,

    @Schema(description = "Indica si el portfolio está activo")
    Boolean isActive
) {}
