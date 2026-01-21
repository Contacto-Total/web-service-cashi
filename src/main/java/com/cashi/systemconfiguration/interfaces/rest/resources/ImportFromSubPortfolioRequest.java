package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.valueobjects.LoadType;

import java.util.List;

/**
 * Resource para importar configuración de cabeceras desde otra subcartera.
 *
 * @param sourceSubPortfolioId ID de la subcartera origen
 * @param loadType Tipo de carga a importar (INICIAL o ACTUALIZACION)
 * @param conflictResolution Cómo resolver conflictos: "SKIP", "REPLACE", "SELECTIVE"
 * @param headersToReplace Lista de headerNames a reemplazar (solo para SELECTIVE)
 */
public record ImportFromSubPortfolioRequest(
        Integer sourceSubPortfolioId,
        LoadType loadType,
        String conflictResolution,
        List<String> headersToReplace
) {
}
