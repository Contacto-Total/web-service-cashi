package com.cashi.collectionmanagement.interfaces.rest.resources;

import java.time.LocalDateTime;

public record StartCallRequest(
        String phoneNumber,
        LocalDateTime startTime
) {
}
