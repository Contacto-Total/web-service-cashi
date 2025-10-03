package com.cashi.collectionmanagement.interfaces.rest.resources;

import java.time.LocalDateTime;

public record EndCallRequest(
        LocalDateTime endTime
) {
}
