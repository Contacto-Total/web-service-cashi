package com.cashi.systemconfiguration.interfaces.rest.resources;

import java.util.List;
import java.util.Map;

/**
 * Recurso de respuesta para la resolución de cabeceras
 */
public record HeaderResolutionResultResource(
    /** Mapeo: nombre en Excel -> nombre interno de la cabecera */
    Map<String, String> resolvedMapping,

    /** Columnas del Excel que no fueron reconocidas */
    List<String> unrecognizedColumns,

    /** Columnas del Excel que están en la lista de ignoradas */
    List<String> ignoredColumns,

    /** Cabeceras configuradas con sus alias */
    List<HeaderWithAliasesResource> configuredHeaders,

    /** Cabeceras obligatorias que no tienen match en el Excel */
    List<String> missingRequiredHeaders,

    /** Indica si hay columnas no reconocidas que requieren acción */
    boolean requiresUserAction
) {
    /**
     * Cabecera con su lista de alias
     */
    public record HeaderWithAliasesResource(
        Integer id,
        String headerName,
        String dataType,
        String displayLabel,
        boolean required,
        List<String> aliases
    ) {}
}
