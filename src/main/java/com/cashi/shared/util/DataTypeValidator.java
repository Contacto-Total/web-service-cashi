package com.cashi.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Utilidad para validación completa de tipos de datos
 * Soporta: TEXTO, NUMERICO, FECHA, EMAIL, TELEFONO, URL, DNI, RUC
 */
public final class DataTypeValidator {

    private static final Logger logger = LoggerFactory.getLogger(DataTypeValidator.class);

    // Patrones de validación
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9\\s\\-\\(\\)]{7,20}$"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?([\\w.-]+)\\.([a-z]{2,})(/.*)?$",
        Pattern.CASE_INSENSITIVE
    );

    // DNI peruano: 8 dígitos
    private static final Pattern DNI_PATTERN = Pattern.compile("^[0-9]{8}$");

    // RUC peruano: 11 dígitos, empieza con 10, 15, 17 o 20
    private static final Pattern RUC_PATTERN = Pattern.compile("^(10|15|17|20)[0-9]{9}$");

    // Carnet de extranjería: Hasta 12 caracteres alfanuméricos
    private static final Pattern CE_PATTERN = Pattern.compile("^[A-Z0-9]{6,12}$", Pattern.CASE_INSENSITIVE);

    private DataTypeValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Resultado de validación con valor convertido
     */
    public static class ValidationResult {
        private final boolean valid;
        private final Object convertedValue;
        private final String errorMessage;

        private ValidationResult(boolean valid, Object convertedValue, String errorMessage) {
            this.valid = valid;
            this.convertedValue = convertedValue;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success(Object value) {
            return new ValidationResult(true, value, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, null, message);
        }

        public boolean isValid() { return valid; }
        public Object getConvertedValue() { return convertedValue; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Valida y convierte un valor según el tipo de dato especificado
     *
     * @param value Valor a validar (puede ser null)
     * @param dataType Tipo de dato (TEXTO, NUMERICO, FECHA, EMAIL, TELEFONO, URL, DNI, RUC, CE)
     * @param format Formato específico (para fechas y números)
     * @param fieldName Nombre del campo (para mensajes de error)
     * @param required Si el campo es obligatorio
     * @return ValidationResult con el valor convertido o mensaje de error
     */
    public static ValidationResult validate(Object value, String dataType, String format,
                                           String fieldName, boolean required) {
        // Verificar si está vacío
        boolean isEmpty = value == null ||
                         (value instanceof String && ((String) value).trim().isEmpty());

        if (isEmpty) {
            if (required) {
                return ValidationResult.failure(
                    "El campo obligatorio '" + fieldName + "' no puede estar vacío"
                );
            }
            return ValidationResult.success(null);
        }

        String strValue = value.toString().trim();
        String dataTypeUpper = dataType != null ? dataType.toUpperCase() : "TEXTO";

        try {
            switch (dataTypeUpper) {
                case "NUMERICO":
                    return validateNumeric(value, strValue, fieldName);

                case "FECHA":
                    return validateDate(value, strValue, format, fieldName);

                case "EMAIL":
                    return validateEmail(strValue, fieldName);

                case "TELEFONO":
                case "PHONE":
                    return validatePhone(strValue, fieldName);

                case "URL":
                    return validateUrl(strValue, fieldName);

                case "DNI":
                    return validateDni(strValue, fieldName);

                case "RUC":
                    return validateRuc(strValue, fieldName);

                case "CE":
                case "CARNET_EXTRANJERIA":
                    return validateCarnetExtranjeria(strValue, fieldName);

                case "TEXTO":
                default:
                    return ValidationResult.success(strValue);
            }
        } catch (Exception e) {
            logger.warn("Error validando campo '{}' con tipo '{}': {}", fieldName, dataType, e.getMessage());
            return ValidationResult.failure(
                "Error validando campo '" + fieldName + "': " + e.getMessage()
            );
        }
    }

    private static ValidationResult validateNumeric(Object value, String strValue, String fieldName) {
        try {
            if (value instanceof Number) {
                return ValidationResult.success(value);
            }

            // Limpiar el string
            String numStr = strValue
                .replace(",", ".")  // Cambiar coma decimal por punto
                .replaceAll("[^0-9.\\-]", ""); // Remover caracteres no numéricos

            if (numStr.isEmpty()) {
                return ValidationResult.success(null);
            }

            // Manejar casos como ".10" o "-.10"
            if (numStr.startsWith(".")) {
                numStr = "0" + numStr;
            } else if (numStr.startsWith("-.")) {
                numStr = "-0" + numStr.substring(1);
            }

            // Intentar parsear como Double
            Double number = Double.parseDouble(numStr);

            // Si es número entero, convertir a Long para evitar decimales innecesarios
            if (number == Math.floor(number) && !Double.isInfinite(number)) {
                return ValidationResult.success(number.longValue());
            }

            return ValidationResult.success(number);
        } catch (NumberFormatException e) {
            return ValidationResult.failure(
                "Valor no numérico para campo '" + fieldName + "': " + strValue
            );
        }
    }

    private static ValidationResult validateDate(Object value, String strValue, String format, String fieldName) {
        try {
            if (value instanceof LocalDate) {
                return ValidationResult.success(value);
            }
            if (value instanceof java.time.LocalDateTime) {
                return ValidationResult.success(((java.time.LocalDateTime) value).toLocalDate());
            }

            LocalDate parsedDate = DateParserUtil.parseFlexibleDate(strValue, format);
            return ValidationResult.success(parsedDate);
        } catch (Exception e) {
            return ValidationResult.failure(
                "Valor no es fecha válida para campo '" + fieldName + "': " + strValue +
                " (formato esperado: " + (format != null ? format : "d/M/yyyy") + ")"
            );
        }
    }

    private static ValidationResult validateEmail(String strValue, String fieldName) {
        if (!EMAIL_PATTERN.matcher(strValue).matches()) {
            return ValidationResult.failure(
                "Email inválido para campo '" + fieldName + "': " + strValue
            );
        }
        return ValidationResult.success(strValue.toLowerCase());
    }

    private static ValidationResult validatePhone(String strValue, String fieldName) {
        // Limpiar el número de teléfono (solo mantener dígitos y +)
        String cleaned = strValue.replaceAll("[^0-9+]", "");

        if (cleaned.length() < 7 || cleaned.length() > 15) {
            return ValidationResult.failure(
                "Teléfono inválido para campo '" + fieldName + "': " + strValue +
                " (debe tener entre 7 y 15 dígitos)"
            );
        }

        return ValidationResult.success(cleaned);
    }

    private static ValidationResult validateUrl(String strValue, String fieldName) {
        if (!URL_PATTERN.matcher(strValue).matches()) {
            return ValidationResult.failure(
                "URL inválida para campo '" + fieldName + "': " + strValue
            );
        }
        // Agregar https:// si no tiene protocolo
        if (!strValue.toLowerCase().startsWith("http://") && !strValue.toLowerCase().startsWith("https://")) {
            strValue = "https://" + strValue;
        }
        return ValidationResult.success(strValue);
    }

    private static ValidationResult validateDni(String strValue, String fieldName) {
        String cleaned = strValue.replaceAll("[^0-9]", "");

        if (!DNI_PATTERN.matcher(cleaned).matches()) {
            return ValidationResult.failure(
                "DNI inválido para campo '" + fieldName + "': " + strValue +
                " (debe tener exactamente 8 dígitos)"
            );
        }
        return ValidationResult.success(cleaned);
    }

    private static ValidationResult validateRuc(String strValue, String fieldName) {
        String cleaned = strValue.replaceAll("[^0-9]", "");

        if (!RUC_PATTERN.matcher(cleaned).matches()) {
            return ValidationResult.failure(
                "RUC inválido para campo '" + fieldName + "': " + strValue +
                " (debe tener 11 dígitos y comenzar con 10, 15, 17 o 20)"
            );
        }
        return ValidationResult.success(cleaned);
    }

    private static ValidationResult validateCarnetExtranjeria(String strValue, String fieldName) {
        String cleaned = strValue.replaceAll("[^A-Za-z0-9]", "").toUpperCase();

        if (!CE_PATTERN.matcher(cleaned).matches()) {
            return ValidationResult.failure(
                "Carnet de extranjería inválido para campo '" + fieldName + "': " + strValue +
                " (debe tener entre 6 y 12 caracteres alfanuméricos)"
            );
        }
        return ValidationResult.success(cleaned);
    }

    /**
     * Verifica si un tipo de dato es soportado
     */
    public static boolean isSupportedDataType(String dataType) {
        if (dataType == null) return false;
        return switch (dataType.toUpperCase()) {
            case "TEXTO", "NUMERICO", "FECHA", "EMAIL", "TELEFONO", "PHONE",
                 "URL", "DNI", "RUC", "CE", "CARNET_EXTRANJERIA" -> true;
            default -> false;
        };
    }
}
