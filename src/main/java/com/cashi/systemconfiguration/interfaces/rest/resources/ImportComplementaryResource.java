package com.cashi.systemconfiguration.interfaces.rest.resources;

import java.util.List;
import java.util.Map;

/**
 * Recurso para importar datos desde un archivo complementario
 */
public record ImportComplementaryResource(
    Integer subPortfolioId,
    Integer complementaryTypeId,  // ID del tipo, o null si se usa typeName
    String typeName,              // Nombre del tipo (alternativa a ID)
    List<Map<String, Object>> data
) {}
