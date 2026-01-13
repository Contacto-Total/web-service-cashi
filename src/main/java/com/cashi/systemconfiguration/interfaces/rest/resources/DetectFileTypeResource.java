package com.cashi.systemconfiguration.interfaces.rest.resources;

/**
 * Recurso para detectar el tipo de archivo basándose en su nombre
 */
public record DetectFileTypeResource(
    Integer subPortfolioId,
    String fileName
) {}
