package com.cashi.collectionmanagement.domain.model.commands;

import java.time.LocalDateTime;

public record EndCallCommand(
    String managementId,
    LocalDateTime endTime
) {
}
