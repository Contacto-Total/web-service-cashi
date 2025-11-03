package com.cashi.customermanagement.interfaces.rest.resources;

public record ImportHistoryResource(
        Long id,
        String fileName,
        String filePath,
        String processedAt,
        String status,
        Integer recordsProcessed,
        String errorMessage
) {
}
