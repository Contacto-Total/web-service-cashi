package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.valueobjects.LoadType;

/**
 * Recurso para crear una nueva cabecera desde una columna no reconocida
 */
public record CreateNewHeaderResource(
    Integer subPortfolioId,
    LoadType loadType,
    String headerName,
    String dataType,        // TEXTO, NUMERICO, FECHA
    String displayLabel,
    String format,
    boolean required,
    String username
) {}
