package com.cashi.shared.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fecha del archivo de carga diaria, tomada de su nombre.
 * La tabla histórica de la carga diaria se nombra con esta fecha (ini_<tabla>_yyyy_MM_dd).
 */
public final class DailyFileDateUtil {

    /** yyyy_MM_dd, yyyy-MM-dd o yyyyMMdd, con el mismo separador y sin dígitos pegados */
    private static final Pattern FECHA_EN_NOMBRE =
            Pattern.compile("(?<!\\d)(\\d{4})([-_]?)(\\d{2})\\2(\\d{2})(?!\\d)");

    private static final DateTimeFormatter FORMATO_TABLA = DateTimeFormatter.ofPattern("yyyy_MM_dd");

    public static final String MENSAJE_FECHA_INVALIDA =
            "El nombre del archivo debe incluir la fecha (yyyy_MM_dd, yyyy-MM-dd o yyyyMMdd). "
                    + "Ej.: Cartera_Castigo_2026_10_17.xlsx";

    private DailyFileDateUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Fecha del nombre del archivo. Vacío si no trae una fecha válida o trae más de una.
     */
    public static Optional<LocalDate> extractDate(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        Matcher m = FECHA_EN_NOMBRE.matcher(fileName);
        if (!m.find()) {
            return Optional.empty();
        }
        String anio = m.group(1);
        String mes = m.group(3);
        String dia = m.group(4);
        if (m.find()) {
            return Optional.empty(); // Más de una fecha: ambiguo
        }
        try {
            return Optional.of(LocalDate.of(Integer.parseInt(anio), Integer.parseInt(mes), Integer.parseInt(dia)));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    /**
     * Fecha del nombre del archivo en el formato de la tabla histórica (yyyy_MM_dd).
     */
    public static Optional<String> extractArchiveDate(String fileName) {
        return extractDate(fileName).map(DailyFileDateUtil::toArchiveDate);
    }

    public static String toArchiveDate(LocalDate date) {
        return date.format(FORMATO_TABLA);
    }
}
