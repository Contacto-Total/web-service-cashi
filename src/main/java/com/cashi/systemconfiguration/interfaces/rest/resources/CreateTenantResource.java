package com.cashi.systemconfiguration.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para crear un nuevo tenant/inquilino")
public record CreateTenantResource(
    @Schema(description = "Código único del tenant", example = "ACME-CORP", required = true)
    String tenantCode,

    @Schema(description = "Nombre del tenant", example = "ACME Corporation", required = true)
    String tenantName,

    @Schema(description = "Razón social completa", example = "ACME Corporation SAC")
    String businessName,

    @Schema(description = "Número de identificación fiscal (RUC)", example = "20123456789")
    String taxId,

    @Schema(description = "Código de país ISO 3166-1 alpha-3", example = "PER")
    String countryCode,

    @Schema(description = "Zona horaria", example = "America/Lima")
    String timezone,

    @Schema(description = "Número máximo de usuarios permitidos", example = "50")
    Integer maxUsers,

    @Schema(description = "Número máximo de sesiones concurrentes", example = "10")
    Integer maxConcurrentSessions,

    @Schema(description = "Nivel de suscripción", example = "PREMIUM")
    String subscriptionTier
) {}
