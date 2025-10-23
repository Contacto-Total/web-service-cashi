package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Request para crear múltiples configuraciones de cabecera en lote")
public record BulkCreateHeaderConfigurationResource(
    @Schema(description = "ID de la subcartera", example = "1", required = true)
    Integer subPortfolioId,

    @Schema(description = "Tipo de carga", example = "ACTUALIZACION", required = true)
    LoadType loadType,

    @Schema(description = "Lista de configuraciones de cabeceras a crear", required = true)
    List<HeaderConfigurationItem> headers
) {
    public record HeaderConfigurationItem(
        @Schema(description = "ID de la definición de campo del catálogo maestro", example = "5", required = true)
        Integer fieldDefinitionId,

        @Schema(description = "Nombre de la cabecera tal como viene del proveedor", example = "DNI", required = true)
        String headerName,

        @Schema(description = "Tipo de dato (TEXTO, NUMERICO, FECHA)", example = "TEXTO", required = true)
        String dataType,

        @Schema(description = "Etiqueta visual para mostrar en UI", example = "Número de Documento", required = true)
        String displayLabel,

        @Schema(description = "Formato específico para esta subcartera", example = "dd/MM/yyyy")
        String format,

        @Schema(description = "Es obligatorio", example = "false")
        Boolean required
    ) {}
}
