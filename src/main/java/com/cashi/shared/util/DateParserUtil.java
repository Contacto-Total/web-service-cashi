package com.cashi.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad centralizada para parseo flexible de fechas
 * Sincronizado con el frontend para garantizar consistencia
 */
public final class DateParserUtil {

    private static final Logger logger = LoggerFactory.getLogger(DateParserUtil.class);

    /**
     * Formatos de fecha soportados, en orden de prioridad
     * IMPORTANTE: Estos formatos deben estar sincronizados con el frontend
     * (header-configuration.component.ts - detectDateFormat)
     */
    public static final List<String> SUPPORTED_DATE_FORMATS = List.of(
        // Formatos día/mes/año (más comunes en Latinoamérica)
        "d/M/yyyy",          // 5/1/2026, 15/12/2026
        "d-M-yyyy",          // 5-1-2026, 15-12-2026
        "d.M.yyyy",          // 5.1.2026, 15.12.2026

        // Formatos año/mes/día (ISO)
        "yyyy-M-d",          // 2026-1-5, 2026-12-15
        "yyyy/M/d",          // 2026/1/5, 2026/12/15

        // Formatos con hora (sin segundos) - comunes en Excel
        "d/M/yyyy H:m",      // 5/1/2026 14:30, 15/01/2026 00:00
        "d-M-yyyy H:m",      // 5-1-2026 14:30
        "yyyy-M-d H:m",      // 2026-1-5 14:30
        "yyyy-M-d'T'H:m",    // 2026-1-5T14:30

        // Formatos con hora (con segundos)
        "d/M/yyyy H:m:s",    // 5/1/2026 14:30:00
        "d-M-yyyy H:m:s",    // 5-1-2026 14:30:00
        "yyyy-M-d H:m:s",    // 2026-1-5 14:30:00
        "yyyy-M-d'T'H:m:s",  // 2026-1-5T14:30:00 (ISO con T)

        // Formatos estrictos (legacy)
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "yyyy-MM-dd",
        "dd/MM/yyyy HH:mm",      // 15/01/2026 00:00
        "dd/MM/yyyy HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss"
    );

    /**
     * Formatos de fecha por defecto para detección automática en frontend
     * Mapeo de regex a formato
     */
    public static final String DEFAULT_DATE_FORMAT = "d/M/yyyy";

    private DateParserUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Parsea una fecha de forma flexible, intentando múltiples formatos comunes.
     * Soporta días y meses de 1 o 2 dígitos (ej: 5/01/2026, 15/1/2026)
     *
     * @param dateStr String con la fecha a parsear
     * @param configuredFormat Formato configurado en la cabecera (puede ser null)
     * @return LocalDate parseado
     * @throws IllegalArgumentException si no se puede parsear con ningún formato
     */
    public static LocalDate parseFlexibleDate(String dateStr, String configuredFormat) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        dateStr = dateStr.trim();

        // Lista de formatos a intentar (en orden de prioridad)
        List<String> formatsToTry = new ArrayList<>();

        // Si hay formato configurado, intentarlo primero (con versión flexible)
        if (configuredFormat != null && !configuredFormat.isEmpty()) {
            // Hacer el formato flexible (d en lugar de dd, M en lugar de MM)
            String flexibleFormat = makeFormatFlexible(configuredFormat);
            formatsToTry.add(flexibleFormat);
            // También agregar el formato original
            if (!flexibleFormat.equals(configuredFormat)) {
                formatsToTry.add(configuredFormat);
            }
        }

        // Agregar formatos comunes como fallback
        formatsToTry.addAll(SUPPORTED_DATE_FORMATS);

        // Intentar cada formato
        for (String format : formatsToTry) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

                // Si el formato incluye hora, parsear como LocalDateTime y extraer fecha
                if (containsTimeComponent(format)) {
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
                    return dateTime.toLocalDate();
                } else {
                    return LocalDate.parse(dateStr, formatter);
                }
            } catch (Exception e) {
                // Continuar con el siguiente formato
                logger.trace("Formato '{}' no coincide con '{}': {}", format, dateStr, e.getMessage());
            }
        }

        // Último intento: formato ISO estándar
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "No se pudo parsear la fecha: " + dateStr +
                ". Formatos soportados: d/M/yyyy, yyyy-M-d, etc."
            );
        }
    }

    /**
     * Convierte un formato estricto a flexible
     * dd/MM/yyyy -> d/M/yyyy
     */
    public static String makeFormatFlexible(String format) {
        if (format == null) return null;
        return format
            .replace("dd", "d")
            .replace("MM", "M")
            .replace("HH", "H")
            .replace("mm", "m")
            .replace("ss", "s");
    }

    /**
     * Verifica si un formato contiene componentes de hora
     */
    private static boolean containsTimeComponent(String format) {
        return format.contains("H") || format.contains("h") ||
               format.contains("m") || format.contains("s");
    }

    /**
     * Formatea una fecha a string usando el formato especificado
     *
     * @param date Fecha a formatear
     * @param format Formato de salida
     * @return String formateado
     */
    public static String formatDate(LocalDate date, String format) {
        if (date == null) return null;
        if (format == null || format.isEmpty()) {
            format = DEFAULT_DATE_FORMAT;
        }
        return date.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * Detecta el formato de una fecha basándose en su estructura
     *
     * @param dateStr String con la fecha
     * @return Formato detectado o null si no se reconoce
     */
    public static String detectFormat(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        dateStr = dateStr.trim();

        // Intentar parsear con cada formato y retornar el primero que funcione
        for (String format : SUPPORTED_DATE_FORMATS) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                if (containsTimeComponent(format)) {
                    LocalDateTime.parse(dateStr, formatter);
                } else {
                    LocalDate.parse(dateStr, formatter);
                }
                return format;
            } catch (Exception e) {
                // Continuar con el siguiente
            }
        }

        return null;
    }
}
