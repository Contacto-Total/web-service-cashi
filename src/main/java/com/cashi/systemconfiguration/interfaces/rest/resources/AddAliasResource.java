package com.cashi.systemconfiguration.interfaces.rest.resources;

/**
 * Recurso para agregar un alias a una cabecera
 */
public record AddAliasResource(
    String alias,
    String username
) {}
