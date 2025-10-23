package com.cashi.systemconfiguration.interfaces.rest.resources;

public record UpdateTypificationConfigCommand(
    String customName,
    Integer customOrder,
    String customIcon,
    String customColor,
    Integer requiresComment,
    Integer minCommentLength,
    Boolean requiresAttachment,
    Boolean requiresFollowupDate,
    String metadataSchema
) {}
