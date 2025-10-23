package com.cashi.systemconfiguration.interfaces.rest.resources;

import java.time.LocalDate;

public record SubPortfolioResource(
    Integer id,
    Integer portfolioId,
    String portfolioCode,
    String portfolioName,
    String subPortfolioCode,
    String subPortfolioName,
    String description,
    Integer isActive,
    LocalDate createdAt,
    LocalDate updatedAt
) {
}
