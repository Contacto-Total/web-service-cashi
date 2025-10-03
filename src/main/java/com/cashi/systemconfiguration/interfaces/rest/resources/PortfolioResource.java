package com.cashi.systemconfiguration.interfaces.rest.resources;

public record PortfolioResource(
    Long id,
    String portfolioCode,
    String portfolioName,
    String portfolioType,
    Long parentPortfolioId,
    Integer hierarchyLevel,
    String description,
    Boolean isActive
) {
}
