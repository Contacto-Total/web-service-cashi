package com.cashi.systemconfiguration.interfaces.rest.resources;

public record PortfolioResource(
    Integer id,
    String portfolioCode,
    String portfolioName,
    String description,
    Integer isActive,
    Boolean hasSubPortfolios
) {
}
