package com.cashi.systemconfiguration.domain.model.enums;

import lombok.Getter;

/**
 * Define la estrategia de clasificaciones que usará cada tenant.
 * Permite total flexibilidad en la configuración de catálogos por cliente.
 */
@Getter
public enum TenantClassificationStrategy {
    /**
     * Usa solo las clasificaciones genéricas del sistema (ContactClassification + ManagementClassification).
     * Ideal para: Clientes pequeños, pruebas, demo.
     */
    GENERIC_ONLY("GENERIC_ONLY", "Solo Clasificaciones Genéricas"),

    /**
     * Usa solo clasificaciones personalizadas (custom) específicas del tenant.
     * Ideal para: Clientes grandes con procesos muy específicos que no se ajustan al estándar.
     */
    CUSTOM_ONLY("CUSTOM_ONLY", "Solo Clasificaciones Personalizadas"),

    /**
     * Usa clasificaciones genéricas + clasificaciones custom adicionales.
     * Ideal para: Clientes que quieren el catálogo base pero necesitan agregar clasificaciones propias.
     */
    HYBRID("HYBRID", "Híbrido (Genéricas + Custom)"),

    /**
     * Sin clasificaciones precargadas. Se configurarán manualmente desde la UI.
     * Ideal para: Tenants que se configurarán completamente desde la interfaz de administración.
     */
    MANUAL("MANUAL", "Configuración Manual");

    private final String code;
    private final String description;

    TenantClassificationStrategy(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Determina si este tenant debe cargar clasificaciones genéricas
     */
    public boolean shouldLoadGeneric() {
        return this == GENERIC_ONLY || this == HYBRID;
    }

    /**
     * Determina si este tenant debe cargar clasificaciones custom
     */
    public boolean shouldLoadCustom() {
        return this == CUSTOM_ONLY || this == HYBRID;
    }

    /**
     * Determina si este tenant NO carga nada (configuración manual)
     */
    public boolean isManual() {
        return this == MANUAL;
    }
}
