package com.cashi.systemconfiguration.interfaces.rest.resources;

import java.util.List;
import java.util.Map;

/**
 * Resource para importar datos de carga diaria
 * Incluye el campo de enlace para vincular con la tabla inicial
 * y el nombre del archivo, cuya fecha nombra la tabla histórica de la carga
 */
public record ImportDailyDataResource(
        Integer subPortfolioId,
        List<Map<String, Object>> data,
        String linkField,
        String fileName
) {
}
