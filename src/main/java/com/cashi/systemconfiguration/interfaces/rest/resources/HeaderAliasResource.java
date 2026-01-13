package com.cashi.systemconfiguration.interfaces.rest.resources;

import java.time.LocalDateTime;

/**
 * Recurso de respuesta para un alias de cabecera
 */
public record HeaderAliasResource(
    Integer id,
    Integer headerConfigurationId,
    String alias,
    boolean isPrincipal,
    LocalDateTime createdAt
) {}
