package com.cashi.collectionmanagement.domain.model.commands;

import java.util.Map;

public record CreateManagementCommand(
    String customerId,
    String advisorId,

    // Multi-tenant fields
    Integer tenantId,
    Integer portfolioId,
    Integer subPortfolioId,
    Long campaignId,  // Campaign usa Long como ID

    // Jerarquía de tipificaciones (3 niveles)
    Integer typificationLevel1Id,
    Integer typificationLevel2Id,
    Integer typificationLevel3Id,

    String observations,
    Map<String, Object> dynamicFields
) {
}
