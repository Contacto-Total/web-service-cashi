package com.cashi.customermanagement.application.internal.commandservices;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import com.cashi.customermanagement.domain.model.entities.FieldTransformationRule;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerRepository;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.FieldTransformationRuleRepository;
import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;

/**
 * Servicio para sincronizar clientes desde tablas dinámicas a tabla clientes
 * Lee datos de tablas ini_<prov>_<car>_<subcartera> y los consolida en la tabla clientes
 */
@Service
@RequiredArgsConstructor
public class CustomerSyncService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final CustomerRepository customerRepository;
    private final SubPortfolioRepository subPortfolioRepository;
    private final FieldTransformationRuleRepository transformationRuleRepository;

    /**
     * Sincroniza clientes desde una tabla específica (método directo)
     */
    @Transactional
    public SyncResult syncCustomersFromTable(String tableName, Long tenantId) {
        System.out.println("🔄 Iniciando sincronización de clientes desde tabla: " + tableName);

        int customersCreated = 0;
        int customersUpdated = 0;
        List<String> errors = new ArrayList<>();

        try {
            // Verificar que la tabla existe
            if (!tableExists(tableName)) {
                throw new IllegalArgumentException("La tabla dinámica no existe: " + tableName);
            }

            // Leer datos de la tabla dinámica
            List<Map<String, Object>> rows = readDynamicTableData(tableName);
            System.out.println("📋 Registros encontrados: " + rows.size());

            // Procesar cada registro
            for (Map<String, Object> row : rows) {
                try {
                    System.out.println("📝 Registro ANTES de transformación: " + row);

                    // Aplicar reglas de transformación de campos
                    Map<String, Object> enrichedRow = applyFieldTransformations(row, tenantId);

                    System.out.println("📝 Registro DESPUÉS de transformación: " + enrichedRow);

                    String identificationCode = getStringValue(enrichedRow, "codigo_identificacion");
                    String document = getStringValue(enrichedRow, "documento");

                    System.out.println("📝 identificationCode=" + identificationCode + ", documento=" + document);

                    if (document == null || document.isEmpty()) {
                        errors.add("Documento vacío en registro");
                        continue;
                    }

                    // Verificar si el cliente ya existe
                    Optional<Customer> existingCustomer = customerRepository.findByTenantIdAndIdentificationCode(
                            tenantId, identificationCode);

                    Customer customer;
                    if (existingCustomer.isPresent()) {
                        // Actualizar cliente existente
                        customer = existingCustomer.get();
                        updateCustomerFromRow(customer, enrichedRow);
                        customersUpdated++;
                    } else {
                        // Crear nuevo cliente
                        customer = createCustomerFromRow(enrichedRow, tenantId, null);
                        customersCreated++;
                    }

                    customerRepository.save(customer);

                } catch (Exception e) {
                    errors.add("Error procesando registro: " + e.getMessage());
                    System.err.println("❌ Error: " + e.getMessage());
                }
            }

            System.out.println("✅ Sincronización completada:");
            System.out.println("   - Clientes creados: " + customersCreated);
            System.out.println("   - Clientes actualizados: " + customersUpdated);
            System.out.println("   - Errores: " + errors.size());

            return new SyncResult(customersCreated, customersUpdated, errors);

        } catch (Exception e) {
            System.err.println("❌ Error fatal en sincronización: " + e.getMessage());
            throw new RuntimeException("Error en sincronización de clientes: " + e.getMessage(), e);
        }
    }

    /**
     * Sincroniza clientes de una sub-cartera específica
     */
    @Transactional
    public SyncResult syncCustomersFromSubPortfolio(Long subPortfolioId) {
        System.out.println("🔄 Iniciando sincronización de clientes para SubPortfolio ID: " + subPortfolioId);

        // 1. Obtener SubPortfolio con sus relaciones
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("SubPortfolio no encontrado: " + subPortfolioId));

        Portfolio portfolio = subPortfolio.getPortfolio();
        Tenant tenant = portfolio.getTenant();

        // 2. Construir nombre de tabla dinámica
        String tableName = buildDynamicTableName(
                tenant.getTenantCode(),
                portfolio.getPortfolioCode(),
                subPortfolio.getSubPortfolioCode()
        );

        System.out.println("📊 Tabla dinámica: " + tableName);

        int customersCreated = 0;
        int customersUpdated = 0;
        List<String> errors = new ArrayList<>();

        try {
            // 3. Verificar que la tabla existe
            if (!tableExists(tableName)) {
                throw new IllegalArgumentException("La tabla dinámica no existe: " + tableName);
            }

            // 4. Leer datos de la tabla dinámica
            List<Map<String, Object>> rows = readDynamicTableData(tableName);
            System.out.println("📋 Registros encontrados: " + rows.size());

            // 5. Procesar cada registro
            for (Map<String, Object> row : rows) {
                try {
                    // Aplicar reglas de transformación de campos
                    Map<String, Object> enrichedRow = applyFieldTransformations(row, tenant.getId().longValue());

                    String identificationCode = getStringValue(enrichedRow, "codigo_identificacion");
                    String document = getStringValue(enrichedRow, "documento");

                    if (document == null || document.isEmpty()) {
                        errors.add("Documento vacío en registro");
                        continue;
                    }

                    // Verificar si el cliente ya existe
                    Optional<Customer> existingCustomer = customerRepository.findByTenantIdAndIdentificationCode(
                            tenant.getId().longValue(), identificationCode);

                    Customer customer;
                    if (existingCustomer.isPresent()) {
                        // Actualizar cliente existente
                        customer = existingCustomer.get();
                        customer.setSubPortfolioId(subPortfolioId.longValue());
                        updateCustomerFromRow(customer, enrichedRow);
                        customersUpdated++;
                    } else {
                        // Crear nuevo cliente
                        customer = createCustomerFromRow(enrichedRow, tenant.getId().longValue(), subPortfolioId.longValue());
                        customersCreated++;
                    }

                    customerRepository.save(customer);

                } catch (Exception e) {
                    errors.add("Error procesando registro: " + e.getMessage());
                    System.err.println("❌ Error: " + e.getMessage());
                }
            }

            System.out.println("✅ Sincronización completada:");
            System.out.println("   - Clientes creados: " + customersCreated);
            System.out.println("   - Clientes actualizados: " + customersUpdated);
            System.out.println("   - Errores: " + errors.size());

            return new SyncResult(customersCreated, customersUpdated, errors);

        } catch (Exception e) {
            System.err.println("❌ Error fatal en sincronización: " + e.getMessage());
            throw new RuntimeException("Error en sincronización de clientes: " + e.getMessage(), e);
        }
    }

    /**
     * Construye el nombre de la tabla dinámica de carga inicial
     */
    private String buildDynamicTableName(String tenantCode, String portfolioCode, String subPortfolioCode) {
        return String.format("ini_%s_%s_%s",
                tenantCode.toLowerCase(),
                portfolioCode.toLowerCase(),
                subPortfolioCode.toLowerCase()
        );
    }

    /**
     * Verifica si una tabla existe en la base de datos
     */
    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT 1 FROM information_schema.tables WHERE table_name = :tableName LIMIT 1";
            var query = entityManager.createNativeQuery(sql);
            query.setParameter("tableName", tableName);
            query.getSingleResult();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lee todos los datos de la tabla dinámica
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readDynamicTableData(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        var query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();

        // Obtener nombres de columnas
        String columnsQuery = "SELECT column_name FROM information_schema.columns WHERE table_name = :tableName ORDER BY ordinal_position";
        var columnsQueryObj = entityManager.createNativeQuery(columnsQuery);
        columnsQueryObj.setParameter("tableName", tableName);
        List<String> columnNames = (List<String>) columnsQueryObj.getResultList();

        // Convertir a lista de mapas
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> rowMap = new HashMap<>();
            for (int i = 0; i < columnNames.size() && i < row.length; i++) {
                rowMap.put(columnNames.get(i), row[i]);
            }
            rows.add(rowMap);
        }

        return rows;
    }

    /**
     * Crea un nuevo cliente desde los datos de la fila
     */
    private Customer createCustomerFromRow(Map<String, Object> row, Long tenantId, Long subPortfolioId) {
        Customer customer = new Customer();

        // Tenant
        customer.setTenantId(tenantId);
        customer.setSubPortfolioId(subPortfolioId);

        // Identificación
        String identificationCode = getStringValue(row, "codigo_identificacion");
        String document = getStringValue(row, "documento");

        customer.setCustomerId(document);  // id_cliente = documento
        customer.setIdentificationCode(identificationCode);
        customer.setDocument(document);

        // Información Personal
        customer.setFullName(getStringValue(row, "nombre_completo"));
        customer.setFirstName(getStringValue(row, "primer_nombre"));
        customer.setSecondName(getStringValue(row, "segundo_nombre"));
        customer.setFirstLastName(getStringValue(row, "primer_apellido"));
        customer.setSecondLastName(getStringValue(row, "segundo_apellido"));

        // Datos Demográficos
        LocalDate birthDate = getDateValue(row, "fecha_nacimiento");
        customer.setBirthDate(birthDate);
        customer.setAge(calculateAge(birthDate));
        customer.setMaritalStatus(getStringValue(row, "estado_civil"));

        // Información Laboral
        customer.setOccupation(getStringValue(row, "ocupacion"));
        customer.setCustomerType(getStringValue(row, "tipo_cliente"));

        // Ubicación
        customer.setAddress(getStringValue(row, "direccion"));
        customer.setDistrict(getStringValue(row, "distrito"));
        customer.setProvince(getStringValue(row, "provincia"));
        customer.setDepartment(getStringValue(row, "departamento"));

        // Referencias
        customer.setPersonalReference(getStringValue(row, "referencia_personal"));

        return customer;
    }

    /**
     * Actualiza un cliente existente desde los datos de la fila
     */
    private void updateCustomerFromRow(Customer customer, Map<String, Object> row) {
        // Actualizar información personal
        customer.setFullName(getStringValue(row, "nombre_completo"));
        customer.setFirstName(getStringValue(row, "primer_nombre"));
        customer.setSecondName(getStringValue(row, "segundo_nombre"));
        customer.setFirstLastName(getStringValue(row, "primer_apellido"));
        customer.setSecondLastName(getStringValue(row, "segundo_apellido"));

        // Actualizar datos demográficos
        LocalDate birthDate = getDateValue(row, "fecha_nacimiento");
        customer.setBirthDate(birthDate);
        customer.setMaritalStatus(getStringValue(row, "estado_civil"));

        // Actualizar información laboral
        customer.setOccupation(getStringValue(row, "ocupacion"));
        customer.setCustomerType(getStringValue(row, "tipo_cliente"));

        // Actualizar ubicación
        customer.setAddress(getStringValue(row, "direccion"));
        customer.setDistrict(getStringValue(row, "distrito"));
        customer.setProvince(getStringValue(row, "provincia"));
        customer.setDepartment(getStringValue(row, "departamento"));

        // Actualizar referencias
        customer.setPersonalReference(getStringValue(row, "referencia_personal"));
    }

    /**
     * Obtiene un valor String de la fila
     */
    private String getStringValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        return value != null ? value.toString().trim() : null;
    }

    /**
     * Obtiene un valor Date de la fila
     */
    private LocalDate getDateValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        if (value == null) return null;

        try {
            if (value instanceof Date) {
                return ((Date) value).toLocalDate();
            } else if (value instanceof java.util.Date) {
                return ((java.util.Date) value).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            } else if (value instanceof LocalDate) {
                return (LocalDate) value;
            } else {
                return LocalDate.parse(value.toString());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error parseando fecha: " + value);
            return null;
        }
    }

    /**
     * Calcula la edad desde la fecha de nacimiento
     */
    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) return null;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Aplica reglas de transformación de campos desde la base de datos
     */
    private Map<String, Object> applyFieldTransformations(Map<String, Object> row, Long tenantId) {
        Map<String, Object> enrichedData = new HashMap<>(row);

        // Obtener todas las reglas activas para este tenant
        List<FieldTransformationRule> rules = transformationRuleRepository
                .findByTenantIdAndIsActiveTrueOrderByRuleOrderAsc(tenantId);

        // Agrupar reglas por campo destino
        Map<String, List<FieldTransformationRule>> rulesByTarget = new HashMap<>();
        for (FieldTransformationRule rule : rules) {
            rulesByTarget.computeIfAbsent(rule.getTargetField(), k -> new ArrayList<>()).add(rule);
        }

        // Aplicar reglas para cada campo destino
        for (Map.Entry<String, List<FieldTransformationRule>> entry : rulesByTarget.entrySet()) {
            String targetField = entry.getKey();
            List<FieldTransformationRule> targetRules = entry.getValue();

            // Verificar si el campo ya existe en los datos
            Object existingValue = enrichedData.get(targetField);
            boolean fieldExists = existingValue != null &&
                                 !existingValue.toString().trim().isEmpty();

            if (!fieldExists && !targetRules.isEmpty()) {
                // Obtener el campo fuente
                String sourceField = targetRules.get(0).getSourceField();
                Object sourceValueObj = enrichedData.get(sourceField);

                if (sourceValueObj != null) {
                    String sourceValue = sourceValueObj.toString();

                    if (!sourceValue.isEmpty()) {
                        // Aplicar reglas hasta que una funcione
                        String derivedValue = applyTransformationRules(sourceValue, targetRules);

                        if (derivedValue != null) {
                            enrichedData.put(targetField, derivedValue);
                            System.out.println("🔄 Campo derivado: " + targetField + " = " + derivedValue +
                                             " (desde " + sourceField + " = " + sourceValue + ")");
                        }
                    }
                }
            }
        }

        return enrichedData;
    }

    /**
     * Aplica reglas de transformación a un valor
     */
    private String applyTransformationRules(String value, List<FieldTransformationRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return value;
        }

        for (FieldTransformationRule rule : rules) {
            // Si no hay condiciones de transformación, copiar el valor tal cual
            if (rule.getStartsWithPrefix() == null &&
                rule.getExtractLastNChars() == null &&
                rule.getRegexPattern() == null) {
                return value;
            }

            // Verificar si coincide con el prefijo
            if (rule.getStartsWithPrefix() != null) {
                if (value.startsWith(rule.getStartsWithPrefix())) {
                    // Aplicar extracción de últimos N caracteres
                    if (rule.getExtractLastNChars() != null && rule.getExtractLastNChars() > 0) {
                        int length = value.length();
                        int charsToExtract = rule.getExtractLastNChars();

                        if (length >= charsToExtract) {
                            return value.substring(length - charsToExtract);
                        }
                    } else {
                        // Si solo tiene prefijo pero no extracción, retornar el valor completo
                        return value;
                    }
                }
            }

            // Aplicar regex si está especificado
            if (rule.getRegexPattern() != null) {
                try {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(rule.getRegexPattern());
                    java.util.regex.Matcher matcher = pattern.matcher(value);

                    if (matcher.find()) {
                        int captureGroup = rule.getRegexCaptureGroup() != null ? rule.getRegexCaptureGroup() : 1;
                        if (matcher.groupCount() >= captureGroup) {
                            return matcher.group(captureGroup);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error aplicando regex: " + e.getMessage());
                }
            }
        }

        return null;  // No se aplicó ninguna regla
    }

    /**
     * Resultado de la sincronización
     */
    public static class SyncResult {
        private final int customersCreated;
        private final int customersUpdated;
        private final List<String> errors;

        public SyncResult(int customersCreated, int customersUpdated, List<String> errors) {
            this.customersCreated = customersCreated;
            this.customersUpdated = customersUpdated;
            this.errors = errors;
        }

        public int getCustomersCreated() {
            return customersCreated;
        }

        public int getCustomersUpdated() {
            return customersUpdated;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public int getTotalCustomers() {
            return customersCreated + customersUpdated;
        }
    }
}
