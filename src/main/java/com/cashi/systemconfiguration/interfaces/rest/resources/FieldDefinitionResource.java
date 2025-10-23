package com.cashi.systemconfiguration.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Definición de campo del catálogo maestro del sistema")
public record FieldDefinitionResource(
    @Schema(description = "ID de la definición", example = "1")
    Integer id,

    @Schema(description = "Código único del campo", example = "documento")
    String fieldCode,

    @Schema(description = "Nombre del campo", example = "Documento de Identidad")
    String fieldName,

    @Schema(description = "Descripción del campo", example = "Número de documento del cliente (DNI, CE, RUC)")
    String description,

    @Schema(description = "Tipo de dato", example = "TEXTO")
    String dataType,

    @Schema(description = "Formato del campo", example = "dd/MM/yyyy")
    String format,

    @Schema(description = "Fecha de creación")
    LocalDate createdAt,

    @Schema(description = "Fecha de actualización")
    LocalDate updatedAt
) {}
