package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.valueobjects.LoadType;

import java.util.List;
import java.util.Map;

/**
 * Recurso para actualizar datos complementarios en la tabla dinámica.
 * Usado para archivos como PKM y Facilidades que actualizan columnas específicas
 * de registros existentes basándose en un campo de enlace.
 */
public record UpdateComplementaryDataResource(
    Integer subPortfolioId,
    LoadType loadType,
    List<Map<String, Object>> data,
    String linkField // Campo usado para identificar el registro a actualizar (ej: COD_CLI)
) {}
