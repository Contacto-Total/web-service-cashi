package com.cashi.systemconfiguration.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dato nuevo del catálogo de campos")
public record CreateFieldDefinitionResource(
    @Schema(description = "Nombre del dato", example = "Rango de maduración")
    String fieldName,

    @Schema(description = "Tipo de dato: TEXTO, NUMERICO, FECHA o BOOLEANO", example = "TEXTO")
    String dataType
) {}
