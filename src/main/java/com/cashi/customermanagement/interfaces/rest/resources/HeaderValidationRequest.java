package com.cashi.customermanagement.interfaces.rest.resources;

public record HeaderValidationRequest(
        String filePath,
        Integer subPortfolioId,
        String loadType  // "INICIAL" or "ACTUALIZACION"
) {
}
