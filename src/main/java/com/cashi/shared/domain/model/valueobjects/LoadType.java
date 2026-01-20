package com.cashi.shared.domain.model.valueobjects;

/**
 * Enum que representa el tipo de carga de datos
 */
public enum LoadType {
    /**
     * Carga inicial del mes - datos completos (tabla maestra de trabajo)
     * Tabla dinámica: <codproveedor>_<codcartera>_<codsubcartera> (sin prefijo)
     * Ejemplo: sam_mas_elm
     * Esta es la tabla principal usada por cashi-discador-backend
     */
    INICIAL("Carga Inicial del Mes", ""),

    /**
     * Carga diaria - actualizaciones (histórico diario)
     * Tabla dinámica: ini_<codproveedor>_<codcartera>_<codsubcartera>
     * Ejemplo: ini_sam_mas_elm
     */
    ACTUALIZACION("Carga Diaria", "ini_");

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
