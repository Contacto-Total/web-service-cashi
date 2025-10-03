package com.cashi.collectionmanagement.domain.model.commands;

import java.time.LocalDateTime;

public record StartCallCommand(
    String managementId,
    String phoneNumber,
    LocalDateTime startTime
) {
}
