package com.cashi.shared.domain.model.valueobjects;

/**
 * Enum que representa el tipo de carga de datos
 */
public enum LoadType {
    /**
     * Carga inicial del mes - datos completos
     * Tabla dinámica: ini_<codproveedor>_<codcartera>_<codsubcartera>
     */
    INICIAL("Carga Inicial del Mes", "ini_"),

    /**
     * Carga diaria - actualizaciones
     * Tabla dinámica: <codproveedor>_<codcartera>_<codsubcartera> (sin prefijo)
     */
    ACTUALIZACION("Carga Diaria", "");

    private final String displayName;
    private final String tablePrefix;

    LoadType(String displayName, String tablePrefix) {
        this.displayName = displayName;
        this.tablePrefix = tablePrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }
}
