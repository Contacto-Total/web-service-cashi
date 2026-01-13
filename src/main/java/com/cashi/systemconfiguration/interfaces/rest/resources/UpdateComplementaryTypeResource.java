package com.cashi.systemconfiguration.interfaces.rest.resources;

import java.util.List;

/**
 * Recurso para actualizar un tipo de archivo complementario
 */
public record UpdateComplementaryTypeResource(
    String typeName,
    String fileNamePattern,
    String linkField,
    List<String> columnsToUpdate,
    String description,
    Boolean isActive
) {}
