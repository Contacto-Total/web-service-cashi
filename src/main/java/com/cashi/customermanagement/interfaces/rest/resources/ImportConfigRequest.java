package com.cashi.customermanagement.interfaces.rest.resources;

public record ImportConfigRequest(
        String watchDirectory,
        String filePattern,
        Integer subPortfolioId,
        Integer checkFrequencyMinutes,
        Boolean active,
        String processedDirectory,
        String errorDirectory,
        Boolean moveAfterProcess
) {
}
