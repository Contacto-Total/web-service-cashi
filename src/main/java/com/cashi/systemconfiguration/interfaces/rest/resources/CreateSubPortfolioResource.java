package com.cashi.systemconfiguration.interfaces.rest.resources;

public record CreateSubPortfolioResource(
    Integer portfolioId,
    String subPortfolioCode,
    String subPortfolioName,
    String description
) {
}
