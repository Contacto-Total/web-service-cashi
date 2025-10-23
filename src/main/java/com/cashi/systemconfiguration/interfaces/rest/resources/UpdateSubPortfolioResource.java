package com.cashi.systemconfiguration.interfaces.rest.resources;

public record UpdateSubPortfolioResource(
    String subPortfolioName,
    String description
) {
}
