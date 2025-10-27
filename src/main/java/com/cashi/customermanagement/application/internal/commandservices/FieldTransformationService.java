package com.cashi.customermanagement.application.internal.commandservices;

import com.cashi.customermanagement.domain.model.entities.FieldTransformationRule;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.FieldTransformationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para aplicar reglas de transformación de campos durante la importación
 * Permite derivar nuevos campos desde campos existentes en el CSV
 */
@Service
@RequiredArgsConstructor
public class FieldTransformationService {

    private final FieldTransformationRuleRepository ruleRepository;

    /**
     * Aplica las reglas de transformación a un mapa de datos
     *
     * @param data Map con los datos del CSV (clave: nombre de columna, valor: dato)
     * @param tenantId ID del tenant
     * @param subPortfolioId ID de la subcartera
     * @return Map con los datos originales más los campos derivados
     */
    public Map<String, Object> applyTransformationRules(
            Map<String, Object> data,
            Long tenantId,
            Integer subPortfolioId) {

        // Crear una copia del mapa original para no modificarlo
        Map<String, Object> transformedData = new HashMap<>(data);

        // Obtener todas las reglas activas para este tenant y subcartera, ordenadas por prioridad
        List<FieldTransformationRule> rules = ruleRepository
                .findByTenantIdAndSubPortfolioIdAndIsActiveTrueOrderByRuleOrderAsc(tenantId, subPortfolioId);

        if (rules.isEmpty()) {
            System.out.println("⚠️ No hay reglas de transformación activas para tenant=" + tenantId +
                             ", subPortfolioId=" + subPortfolioId);
            return transformedData;
        }

        System.out.println("🔄 Aplicando " + rules.size() + " reglas de transformación...");

        // Aplicar cada regla en orden
        for (FieldTransformationRule rule : rules) {
            try {
                applyRule(rule, transformedData);
            } catch (Exception e) {
                System.err.println("❌ Error aplicando regla ID=" + rule.getId() +
                                 " (" + rule.getDescription() + "): " + e.getMessage());
                // Continuar con la siguiente regla aunque una falle
            }
        }

        return transformedData;
    }

    /**
     * Aplica una regla de transformación individual
     */
    private void applyRule(FieldTransformationRule rule, Map<String, Object> data) {
        String sourceValue = getStringValue(data, rule.getSourceField());

        // Si no existe el campo origen o está vacío, no hacer nada
        if (sourceValue == null || sourceValue.trim().isEmpty()) {
            System.out.println("⏭️  Regla ignorada (campo origen vacío): " + rule.getSourceField() +
                             " → " + rule.getTargetField());
            return;
        }

        // Verificar prefijo si está configurado
        if (rule.getStartsWithPrefix() != null && !rule.getStartsWithPrefix().isEmpty()) {
            if (!sourceValue.startsWith(rule.getStartsWithPrefix())) {
                System.out.println("⏭️  Regla ignorada (prefijo no coincide): " + sourceValue +
                                 " no empieza con '" + rule.getStartsWithPrefix() + "'");
                return;
            }
        }

        String transformedValue = sourceValue;

        // Aplicar transformación según el método configurado
        if (rule.getRegexPattern() != null && !rule.getRegexPattern().isEmpty()) {
            // Método 1: Usar regex para extraer
            transformedValue = applyRegexExtraction(sourceValue, rule);
        } else if (rule.getExtractLastNChars() != null && rule.getExtractLastNChars() > 0) {
            // Método 2: Extraer últimos N caracteres
            transformedValue = extractLastNChars(sourceValue, rule.getExtractLastNChars());
        }

        // Guardar el valor transformado en el campo destino
        if (transformedValue != null && !transformedValue.isEmpty()) {
            data.put(rule.getTargetField(), transformedValue);
            System.out.println("✅ Transformación aplicada: " + rule.getSourceField() +
                             " [" + sourceValue + "] → " + rule.getTargetField() +
                             " [" + transformedValue + "]");
        } else {
            System.out.println("⚠️ Transformación resultó en valor vacío para regla: " + rule.getDescription());
        }
    }

    /**
     * Aplica extracción mediante expresión regular
     */
    private String applyRegexExtraction(String value, FieldTransformationRule rule) {
        try {
            Pattern pattern = Pattern.compile(rule.getRegexPattern());
            Matcher matcher = pattern.matcher(value);

            if (matcher.find()) {
                int captureGroup = rule.getRegexCaptureGroup() != null ?
                                   rule.getRegexCaptureGroup() : 1;

                if (matcher.groupCount() >= captureGroup) {
                    String extracted = matcher.group(captureGroup);
                    System.out.println("🔍 Regex match: '" + value + "' → grupo " +
                                     captureGroup + " = '" + extracted + "'");
                    return extracted;
                }
            }

            System.out.println("⚠️ Regex no coincidió: patrón='" + rule.getRegexPattern() +
                             "' valor='" + value + "'");
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error en regex: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extrae los últimos N caracteres de una cadena
     */
    private String extractLastNChars(String value, int n) {
        if (value.length() >= n) {
            String extracted = value.substring(value.length() - n);
            System.out.println("✂️  Extrayendo últimos " + n + " caracteres: '" +
                             value + "' → '" + extracted + "'");
            return extracted;
        } else {
            System.out.println("⚠️ Valor muy corto para extraer " + n + " caracteres: '" +
                             value + "' (longitud: " + value.length() + ")");
            return value; // Devolver el valor completo si es más corto
        }
    }

    /**
     * Obtiene un valor como String del mapa de datos
     */
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }
}
