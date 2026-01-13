package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.valueobjects.LoadType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para crear una configuración de cabecera")
public record CreateHeaderConfigurationResource(
    @Schema(description = "ID de la subcartera", example = "1", required = true)
    Integer subPortfolioId,

    @Schema(description = "ID de la definición de campo del catálogo maestro", example = "5", required = true)
    Integer fieldDefinitionId,

    @Schema(description = "Nombre de la cabecera tal como viene del proveedor", example = "DNI", required = true)
    String headerName,

    @Schema(description = "Tipo de dato", example = "TEXTO")
    String dataType,

    @Schema(description = "Etiqueta visual para mostrar en UI", example = "Número de Documento", required = true)
    String displayLabel,

    @Schema(description = "Formato específico para esta subcartera", example = "dd/MM/yyyy")
    String format,

    @Schema(description = "Es obligatorio", example = "false")
    Boolean required,

    @Schema(description = "Tipo de carga", example = "ACTUALIZACION", required = true)
    LoadType loadType,

    @Schema(description = "Campo origen para transformación")
    String sourceField,

    @Schema(description = "Patrón regex para extraer valor")
    String regexPattern
) {}
