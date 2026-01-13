package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.valueobjects.LoadType;

import java.util.List;

/**
 * Recurso para solicitar resolución de cabeceras
 */
public record ResolveHeadersResource(
    Integer subPortfolioId,
    LoadType loadType,
    List<String> excelHeaders
) {}
