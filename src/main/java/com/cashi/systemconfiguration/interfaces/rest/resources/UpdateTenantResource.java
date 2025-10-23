package com.cashi.systemconfiguration.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para actualizar un tenant/inquilino existente")
public record UpdateTenantResource(
    @Schema(description = "Nombre del tenant", example = "ACME Corporation Updated")
    String tenantName,

    @Schema(description = "Razón social completa", example = "ACME Corporation SAC")
    String businessName,

    @Schema(description = "Número de identificación fiscal (RUC)", example = "20123456789")
    String taxId,

    @Schema(description = "Código de país ISO 3166-1 alpha-3", example = "PER")
    String countryCode,

    @Schema(description = "Zona horaria", example = "America/Lima")
    String timezone,

    @Schema(description = "Número máximo de usuarios permitidos", example = "100")
    Integer maxUsers,

    @Schema(description = "Número máximo de sesiones concurrentes", example = "20")
    Integer maxConcurrentSessions,

    @Schema(description = "Nivel de suscripción", example = "ENTERPRISE")
    String subscriptionTier,

    @Schema(description = "Estado activo/inactivo", example = "true")
    Boolean isActive
) {}
