package com.cashi.shared.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utilidad para sanitizar nombres de tablas y columnas SQL
 * Previene ataques de SQL Injection al validar identificadores dinámicos
 */
public final class SqlSanitizer {

    // Patrón para identificadores SQL válidos: solo letras, números y guiones bajos
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    // Longitud máxima para identificadores SQL (MySQL limit es 64)
    private static final int MAX_IDENTIFIER_LENGTH = 64;

    // Palabras reservadas de SQL que no deben usarse como identificadores
    private static final Set<String> SQL_RESERVED_WORDS = Set.of(
        "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE",
        "TABLE", "INDEX", "VIEW", "DATABASE", "SCHEMA", "GRANT", "REVOKE",
        "WHERE", "FROM", "JOIN", "UNION", "ORDER", "GROUP", "HAVING", "LIMIT",
        "AND", "OR", "NOT", "NULL", "TRUE", "FALSE", "AS", "ON", "IN", "IS",
        "LIKE", "BETWEEN", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END",
        "PRIMARY", "FOREIGN", "KEY", "REFERENCES", "CONSTRAINT", "UNIQUE",
        "INTO", "VALUES", "SET", "ALL", "DISTINCT", "TOP", "PERCENT",
        "EXEC", "EXECUTE", "DECLARE", "CURSOR", "FETCH", "OPEN", "CLOSE"
    );

    private SqlSanitizer() {
        // Utility class - prevent instantiation
    }

    /**
     * Sanitiza un nombre de tabla para uso seguro en SQL dinámico
     *
     * @param tableName Nombre de la tabla a sanitizar
     * @return Nombre sanitizado
     * @throws IllegalArgumentException si el nombre es inválido o potencialmente peligroso
     */
    public static String sanitizeTableName(String tableName) {
        return sanitizeIdentifier(tableName, "tabla");
    }

    /**
     * Sanitiza un nombre de columna para uso seguro en SQL dinámico
     *
     * @param columnName Nombre de la columna a sanitizar
     * @return Nombre sanitizado
     * @throws IllegalArgumentException si el nombre es inválido o potencialmente peligroso
     */
    public static String sanitizeColumnName(String columnName) {
        return sanitizeIdentifier(columnName, "columna");
    }

    /**
     * Sanitiza un identificador SQL genérico
     *
     * @param identifier El identificador a sanitizar
     * @param type Tipo de identificador (para mensajes de error)
     * @return Identificador sanitizado
     * @throws IllegalArgumentException si el identificador es inválido
     */
    private static String sanitizeIdentifier(String identifier, String type) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de " + type + " no puede estar vacío");
        }

        String sanitized = identifier.trim().toLowerCase();

        // Validar longitud
        if (sanitized.length() > MAX_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException(
                "El nombre de " + type + " excede la longitud máxima de " + MAX_IDENTIFIER_LENGTH + " caracteres: " + sanitized
            );
        }

        // Validar patrón de identificador válido
        if (!VALID_IDENTIFIER.matcher(sanitized).matches()) {
            throw new IllegalArgumentException(
                "El nombre de " + type + " contiene caracteres inválidos: " + sanitized +
                ". Solo se permiten letras, números y guiones bajos, y debe comenzar con letra o guión bajo."
            );
        }

        // Verificar palabras reservadas
        if (SQL_RESERVED_WORDS.contains(sanitized.toUpperCase())) {
            throw new IllegalArgumentException(
                "El nombre de " + type + " es una palabra reservada de SQL: " + sanitized
            );
        }

        return sanitized;
    }

    /**
     * Convierte un nombre de cabecera a un nombre de columna SQL seguro
     * Normaliza el nombre eliminando caracteres especiales y acentos
     *
     * @param headerName Nombre de cabecera original
     * @return Nombre de columna sanitizado
     */
    public static String headerToColumnName(String headerName) {
        if (headerName == null || headerName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de cabecera no puede estar vacío");
        }

        String normalized = headerName
                .toLowerCase()
                .trim()
                // Normalizar acentos
                .replaceAll("[áàäâã]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöôõ]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("[ñ]", "n")
                .replaceAll("[ç]", "c")
                // Reemplazar espacios y caracteres especiales por guión bajo
                .replaceAll("[^a-z0-9]", "_")
                // Eliminar guiones bajos duplicados
                .replaceAll("_+", "_")
                // Eliminar guiones bajos al inicio o final
                .replaceAll("^_|_$", "");

        // Si después de normalizar queda vacío, usar un nombre por defecto
        if (normalized.isEmpty()) {
            normalized = "col_" + System.currentTimeMillis();
        }

        // Si comienza con número, agregar prefijo
        if (Character.isDigit(normalized.charAt(0))) {
            normalized = "col_" + normalized;
        }

        // Truncar si excede el límite
        if (normalized.length() > MAX_IDENTIFIER_LENGTH) {
            normalized = normalized.substring(0, MAX_IDENTIFIER_LENGTH);
        }

        return normalized;
    }

    /**
     * Valida que un nombre de tabla sea seguro sin modificarlo
     *
     * @param tableName Nombre de la tabla
     * @return true si es válido, false si no
     */
    public static boolean isValidTableName(String tableName) {
        try {
            sanitizeTableName(tableName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Valida que un nombre de columna sea seguro sin modificarlo
     *
     * @param columnName Nombre de la columna
     * @return true si es válido, false si no
     */
    public static boolean isValidColumnName(String columnName) {
        try {
            sanitizeColumnName(columnName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
