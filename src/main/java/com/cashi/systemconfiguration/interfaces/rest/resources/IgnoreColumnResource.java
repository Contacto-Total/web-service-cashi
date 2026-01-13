package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.valueobjects.LoadType;

/**
 * Recurso para marcar una columna como ignorada
 */
public record IgnoreColumnResource(
    Integer subPortfolioId,
    LoadType loadType,
    String columnName,
    String username
) {}
