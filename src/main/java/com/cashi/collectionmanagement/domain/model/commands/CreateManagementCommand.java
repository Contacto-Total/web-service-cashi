package com.cashi.collectionmanagement.domain.model.commands;

public record CreateManagementCommand(
    String customerId,
    String advisorId,

    // Multi-tenant fields
    Integer tenantId,
    Integer portfolioId,
    Integer subPortfolioId,

    // Contact info
    String phone,

    // Hierarchical categorization (3 levels)
    Long level1Id,
    String level1Name,
    Long level2Id,
    String level2Name,
    Long level3Id,
    String level3Name,

    String observations
) {
}
