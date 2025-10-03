package com.cashi.systemconfiguration.interfaces.rest.resources;

public record TenantClassificationConfigResource(
    Long id,
    Long tenantId,
    Long portfolioId,
    ClassificationCatalogResource classification,
    Boolean isEnabled,
    Boolean isRequired,
    String customName,
    Integer customOrder,
    String customIcon,
    String customColor,
    Boolean requiresComment,
    Integer minCommentLength,
    Boolean requiresAttachment,
    Boolean requiresFollowupDate,
    String effectiveName,
    Integer effectiveOrder,
    String effectiveIcon,
    String effectiveColor
) {}
