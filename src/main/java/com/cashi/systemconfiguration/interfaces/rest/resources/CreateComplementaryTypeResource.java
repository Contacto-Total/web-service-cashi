package com.cashi.systemconfiguration.interfaces.rest.resources;

import java.util.List;

/**
 * Recurso para crear un tipo de archivo complementario
 */
public record CreateComplementaryTypeResource(
    Integer subPortfolioId,
    String typeName,
    String fileNamePattern,
    String linkField,
    List<String> columnsToUpdate,
    String description
) {}
