package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.valueobjects.LoadType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para actualizar una configuración de cabecera")
public record UpdateHeaderConfigurationResource(
    @Schema(description = "Etiqueta visual para mostrar en UI", example = "Número de Documento")
    String displayLabel,

    @Schema(description = "Formato específico para esta subcartera", example = "dd/MM/yyyy")
    String format,

    @Schema(description = "Es obligatorio", example = "false")
    Boolean required,

    @Schema(description = "Tipo de carga", example = "ACTUALIZACION")
    LoadType loadType
) {}
