package com.cashi.systemconfiguration.interfaces.rest.resources;

public record UpdateClassificationConfigCommand(
    String customName,
    Integer customOrder,
    String customIcon,
    String customColor,
    Boolean requiresComment,
    Integer minCommentLength,
    Boolean requiresAttachment,
    Boolean requiresFollowupDate,
    String metadataSchema
) {}
