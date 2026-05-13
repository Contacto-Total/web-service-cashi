package com.cashi.osiptelvalidation.domain.model.valueobjects;

/**
 * Operador móvil reportado por Osiptel.
 * OTRO captura operadores menores (Cuy, Inkacel, etc.) o casos donde el portal devuelve un nombre que no coincide con los cuatro principales.
 */
public enum OperatorCode {
    CLARO,
    MOVISTAR,
    ENTEL,
    BITEL,
    OTRO;

    /**
     * Normaliza el texto que devuelve el portal al enum.
     * Si llega null o no reconocido, retorna OTRO.
     */
    public static OperatorCode fromPortalText(String raw) {
        if (raw == null) {
            return OTRO;
        }
        String normalized = raw.trim().toUpperCase();
        if (normalized.contains("CLARO")) return CLARO;
        if (normalized.contains("MOVISTAR")) return MOVISTAR;
        if (normalized.contains("ENTEL")) return ENTEL;
        if (normalized.contains("BITEL")) return BITEL;
        return OTRO;
    }
}
