package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.valueobjects.LoadType;

import java.time.LocalDateTime;

/**
 * Recurso de respuesta para historial de cambios de cabeceras
 */
public record HeaderChangeHistoryResource(
    Integer id,
    Integer subPortfolioId,
    LoadType loadType,
    String changeType,
    String excelColumnName,
    String internalHeaderName,
    Integer headerConfigurationId,
    String username,
    LocalDateTime changedAt
) {}
