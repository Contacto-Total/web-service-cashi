package com.cashi.customermanagement.interfaces.rest.resources;

public record ImportConfigRequest(
        String watchDirectory,
        String filePattern,
        Integer subPortfolioId,
        Integer checkFrequencyMinutes,
        String scheduledTime, // Formato: "HH:mm:ss" (ej: "02:00:00")
        Boolean active,
        String processedDirectory,
        String errorDirectory,
        Boolean moveAfterProcess
) {
}
