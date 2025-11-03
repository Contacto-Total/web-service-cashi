package com.cashi.customermanagement.interfaces.rest.resources;

public record ImportConfigResource(
        Long id,
        String watchDirectory,
        String filePattern,
        Integer subPortfolioId,
        Integer checkFrequencyMinutes,
        Boolean active,
        String processedDirectory,
        String errorDirectory,
        Boolean moveAfterProcess,
        String lastCheckAt
) {
}
