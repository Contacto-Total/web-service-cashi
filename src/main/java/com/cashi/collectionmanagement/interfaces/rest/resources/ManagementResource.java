package com.cashi.collectionmanagement.interfaces.rest.resources;

public record ManagementResource(
        Long id,
        String customerId,
        String advisorId,

        // Multi-tenant fields
        Integer tenantId,
        String tenantName,
        Integer portfolioId,
        String portfolioName,
        Integer subPortfolioId,
        String subPortfolioName,

        // Contact info
        String phone,

        // Hierarchical categorization
        Long level1Id,
        String level1Name,
        Long level2Id,
        String level2Name,
        Long level3Id,
        String level3Name,

        String observations,
        Boolean typificationRequiresPayment,
        Boolean typificationRequiresSchedule,

        // Automatic timestamp fields
        String managementDate,  // Fecha de gestión (YYYY-MM-DD)
        String managementTime   // Hora de gestión (HH:mm:ss)
) {
}
