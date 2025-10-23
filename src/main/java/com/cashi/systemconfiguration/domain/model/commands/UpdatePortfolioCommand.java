package com.cashi.systemconfiguration.domain.model.commands;

/**
 * Command to update an existing Portfolio
 */
public record UpdatePortfolioCommand(
    String portfolioName,
    String description,
    Boolean isActive
) {
}
