package com.cashi.osiptelvalidation.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * Número de teléfono móvil peruano normalizado (9XXXXXXXX).
 * Inmutable. Lanza IllegalArgumentException si la entrada no es móvil PE.
 *
 * Excluye líneas fijas (7 dígitos): Osiptel solo valida móvil para este flujo.
 */
public final class PhoneNumber {

    private static final Pattern PE_MOBILE = Pattern.compile("^9\\d{8}$");

    private final String value;

    private PhoneNumber(String value) {
        this.value = value;
    }

    public static PhoneNumber of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("phone no puede ser null");
        }
        String cleaned = raw.trim().replaceAll("[\\s\\-()]", "");
        // Tolerar prefijo internacional opcional (+51 o 51 al inicio)
        if (cleaned.startsWith("+51")) {
            cleaned = cleaned.substring(3);
        } else if (cleaned.startsWith("51") && cleaned.length() == 11) {
            cleaned = cleaned.substring(2);
        }
        if (!PE_MOBILE.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("phone inválido (esperado móvil PE 9XXXXXXXX): " + raw);
        }
        return new PhoneNumber(cleaned);
    }

    public static boolean isValid(String raw) {
        try {
            of(raw);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String value() {
        return value;
    }

    /** Versión enmascarada para logs (XXXXX*321). */
    public String masked() {
        if (value.length() < 3) return "***";
        return "******" + value.substring(value.length() - 3);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof PhoneNumber other) && other.value.equals(this.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
