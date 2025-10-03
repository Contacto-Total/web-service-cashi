package com.cashi.collectionmanagement.interfaces.rest.resources;

import java.time.LocalDateTime;

public record CallDetailResource(
        String phoneNumber,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationSeconds
) {
}
