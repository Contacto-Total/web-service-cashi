package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.valueobjects.LoadType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Configuración de cabecera personalizada")
public record HeaderConfigurationResource(
    @Schema(description = "ID de la configuración", example = "1")
    Integer id,

    @Schema(description = "ID de la subcartera", example = "1")
    Integer subPortfolioId,

    @Schema(description = "ID de la definición de campo del catálogo maestro", example = "5")
    Integer fieldDefinitionId,

    @Schema(description = "Nombre de la cabecera tal como viene del proveedor", example = "DNI")
    String headerName,

    @Schema(description = "Tipo de dato", example = "TEXTO")
    String dataType,

    @Schema(description = "Etiqueta visual para mostrar en UI", example = "Número de Documento")
    String displayLabel,

    @Schema(description = "Formato específico para esta subcartera", example = "dd/MM/yyyy")
    String format,

    @Schema(description = "Es obligatorio", example = "false")
    Boolean required,

    @Schema(description = "Tipo de carga", example = "ACTUALIZACION")
    LoadType loadType,

    @Schema(description = "Fecha de creación")
    LocalDate createdAt,

    @Schema(description = "Fecha de actualización")
    LocalDate updatedAt
) {}
