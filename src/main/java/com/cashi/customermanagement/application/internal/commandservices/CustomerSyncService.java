package com.cashi.customermanagement.application.internal.commandservices;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import com.cashi.customermanagement.domain.model.entities.ContactMethod;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ContactMethodRepository;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerRepository;
import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.HeaderConfigurationRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.Collections;

/**
 * Servicio para sincronizar clientes desde tablas dinámicas a tabla clientes
 * Lee datos de tablas ini_<prov>_<car>_<subcartera> y los consolida en la tabla clientes
 *
 * MODO PRUEBA: Configurar app.customer-sync.test-mode=true para escribir en cliente_prueba
 */
@Service
public class CustomerSyncService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final CustomerRepository customerRepository;
    private final ContactMethodRepository contactMethodRepository;
    private final SubPortfolioRepository subPortfolioRepository;
    private final HeaderConfigurationRepository headerConfigurationRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Modo de prueba: cuando es true, escribe en cliente_prueba en lugar de clientes
     */
    @Value("${app.customer-sync.test-mode:false}")
    private boolean testMode;

    /**
     * Nombre de la tabla de prueba para clientes
     */
    @Value("${app.customer-sync.test-table:cliente_prueba}")
    private String testTableName;

    /**
     * Nombre de la tabla de prueba para métodos de contacto
     */
    @Value("${app.customer-sync.test-contact-table:metodos_contacto_prueba}")
    private String testContactTableName;

    public CustomerSyncService(
            EntityManager entityManager,
            CustomerRepository customerRepository,
            ContactMethodRepository contactMethodRepository,
            SubPortfolioRepository subPortfolioRepository,
            HeaderConfigurationRepository headerConfigurationRepository,
            JdbcTemplate jdbcTemplate) {
        this.entityManager = entityManager;
        this.customerRepository = customerRepository;
        this.contactMethodRepository = contactMethodRepository;
        this.subPortfolioRepository = subPortfolioRepository;
        this.headerConfigurationRepository = headerConfigurationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

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

            // ========== OPTIMIZACIÓN: CARGAR CLIENTES EXISTENTES DE UNA VEZ ==========
            List<Customer> existingCustomers = customerRepository.findByTenantId(tenantId);

            // Crear un Map para búsqueda O(1) por codigo_identificacion
            Map<String, Customer> existingCustomersMap = new HashMap<>();
            for (Customer c : existingCustomers) {
                if (c.getIdentificationCode() != null) {
                    existingCustomersMap.put(c.getIdentificationCode(), c);
                }
            }

            // Listas para batch operations
            List<Customer> customersToSave = new ArrayList<>();
            List<Map<String, Object>> rowsToSync = new ArrayList<>(); // Para sincronizar contactos después

            // Procesar cada registro (preparación sin save individual)
            for (Map<String, Object> row : rows) {
                try {
                    String identificationCode = getStringValue(row, "codigo_identificacion");
                    String document = getStringValue(row, "documento");

                    if (document == null || document.isEmpty()) {
                        errors.add("Documento vacío en registro");
                        continue;
                    }

                    // Buscar en Map en lugar de query a BD
                    Customer existingCustomer = existingCustomersMap.get(identificationCode);

                    Customer customer;
                    if (existingCustomer != null) {
                        // Actualizar cliente existente
                        customer = existingCustomer;
                        customer.setTenantId(tenantId);
                        // Note: portfolioId y subPortfolioId quedarán null ya que este método directo no los tiene
                        updateCustomerFromRow(customer, row);
                        customersUpdated++;
                    } else {
                        // Crear nuevo cliente (sin portfolio/subportfolio ya que este método directo no los tiene)
                        customer = createCustomerFromRowLegacy(row, tenantId);
                        customersCreated++;
                    }

                    customersToSave.add(customer);
                    rowsToSync.add(row);

                } catch (Exception e) {
                    errors.add("Error procesando registro: " + e.getMessage());
                }
            }

            // ========== BATCH SAVE: Guardar todos los clientes de una vez ==========
            if (!customersToSave.isEmpty()) {
                customerRepository.saveAll(customersToSave);

                // ========== SINCRONIZAR CONTACTOS DESPUÉS DEL BATCH SAVE ==========
                int contactsCreated = 0;
                for (int i = 0; i < customersToSave.size(); i++) {
                    Customer customer = customersToSave.get(i);
                    Map<String, Object> row = rowsToSync.get(i);
                    try {
                        contactsCreated += syncCustomerContacts(customer, row);
                    } catch (Exception e) {
                        errors.add("Error sincronizando contactos para " + customer.getIdentificationCode() + ": " + e.getMessage());
                    }
                }
                System.out.println("📞 Contactos creados: " + contactsCreated);
            }

            return new SyncResult(customersCreated, customersUpdated, errors);

        } catch (Exception e) {
            System.err.println("❌ Error fatal en sincronización: " + e.getMessage());
            throw new RuntimeException("Error en sincronización de clientes: " + e.getMessage(), e);
        }
    }

    /**
     * Sincroniza clientes de una sub-cartera específica con un LoadType específico
     */
    @Transactional
    public SyncResult syncCustomersFromSubPortfolio(Long subPortfolioId, LoadType loadType) {
        System.out.println("🔄 Iniciando sincronización de clientes para SubPortfolio ID: " + subPortfolioId + ", LoadType: " + loadType);

        // 1. Obtener SubPortfolio con sus relaciones
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("SubPortfolio no encontrado: " + subPortfolioId));

        Portfolio portfolio = subPortfolio.getPortfolio();
        Tenant tenant = portfolio.getTenant();

        // 2. Construir nombre de tabla dinámica
        String tableName = buildDynamicTableName(
                tenant.getTenantCode(),
                portfolio.getPortfolioCode(),
                subPortfolio.getSubPortfolioCode(),
                loadType
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

            // ========== OPTIMIZACIÓN: CARGAR CLIENTES EXISTENTES DE UNA VEZ ==========
            // En lugar de hacer 4063 queries individuales, cargamos todos los clientes del tenant de una vez
            List<Customer> existingCustomers = customerRepository.findByTenantId(tenant.getId().longValue());

            // Crear un Map para búsqueda O(1) por codigo_identificacion
            Map<String, Customer> existingCustomersMap = new HashMap<>();
            for (Customer c : existingCustomers) {
                if (c.getIdentificationCode() != null) {
                    existingCustomersMap.put(c.getIdentificationCode(), c);
                }
            }

            // Listas para batch operations
            List<Customer> customersToSave = new ArrayList<>();
            List<Map<String, Object>> rowsToSync = new ArrayList<>(); // Para sincronizar contactos después

            // 5. Procesar cada registro (preparación sin save individual)
            for (Map<String, Object> row : rows) {
                try {
                    // Mapear columnas de financiera a sistema usando HeaderConfiguration
                    Map<String, Object> mappedRow = mapColumnsToSystemFields(row, subPortfolio, loadType);

                    String identificationCode = getStringValue(mappedRow, "codigo_identificacion");
                    String document = getStringValue(mappedRow, "documento");

                    if (document == null || document.isEmpty()) {
                        errors.add("Documento vacío en registro");
                        continue;
                    }

                    // Buscar en Map en lugar de query a BD (O(1) vs O(n))
                    Customer existingCustomer = existingCustomersMap.get(identificationCode);

                    Customer customer;
                    if (existingCustomer != null) {
                        // Actualizar cliente existente
                        customer = existingCustomer;
                        // Actualizar jerarquía completa
                        customer.setTenantId(tenant.getId().longValue());
                        customer.setTenantName(tenant.getTenantName());
                        customer.setPortfolioId(portfolio.getId().longValue());
                        customer.setPortfolioName(portfolio.getPortfolioName());
                        customer.setSubPortfolioId(subPortfolioId.longValue());
                        customer.setSubPortfolioName(subPortfolio.getSubPortfolioName());
                        updateCustomerFromRow(customer, mappedRow);
                        customersUpdated++;
                    } else {
                        // Crear nuevo cliente
                        customer = createCustomerFromRow(mappedRow, tenant, portfolio, subPortfolio);
                        customersCreated++;
                    }

                    customersToSave.add(customer);
                    rowsToSync.add(mappedRow);

                } catch (Exception e) {
                    errors.add("Error procesando registro: " + e.getMessage());
                }
            }

            // ========== BATCH SAVE: Guardar todos los clientes ==========
            if (!customersToSave.isEmpty()) {
                if (testMode) {
                    // MODO PRUEBA: Escribir en tablas de prueba usando JDBC
                    System.out.println("🧪 MODO PRUEBA ACTIVO: Escribiendo en tabla '" + testTableName + "'");
                    Map<String, Long> savedIdsMap = saveCustomersToTestTableWithIds(customersToSave, rowsToSync);
                    System.out.println("✅ " + savedIdsMap.size() + " clientes guardados en tabla de prueba");

                    // Sincronizar contactos a tabla de prueba
                    int contactsCreated = 0;
                    for (int i = 0; i < customersToSave.size(); i++) {
                        Customer customer = customersToSave.get(i);
                        Map<String, Object> row = rowsToSync.get(i);
                        String identificationCode = customer.getIdentificationCode();

                        // Obtener el ID del cliente en la tabla de prueba
                        Long testClientId = savedIdsMap.get(identificationCode);
                        if (testClientId != null) {
                            try {
                                contactsCreated += syncCustomerContactsToTestTable(testClientId, row);
                            } catch (Exception e) {
                                errors.add("Error sincronizando contactos para " + identificationCode + ": " + e.getMessage());
                            }
                        }
                    }
                    System.out.println("📞 " + contactsCreated + " contactos guardados en tabla " + testContactTableName);
                } else {
                    // MODO PRODUCCIÓN: Usar JPA repository
                    customerRepository.saveAll(customersToSave);

                    // ========== SINCRONIZAR CONTACTOS DESPUÉS DEL BATCH SAVE ==========
                    int contactsCreated = 0;
                    for (int i = 0; i < customersToSave.size(); i++) {
                        Customer customer = customersToSave.get(i);
                        Map<String, Object> row = rowsToSync.get(i);
                        try {
                            contactsCreated += syncCustomerContacts(customer, row);
                        } catch (Exception e) {
                            errors.add("Error sincronizando contactos para " + customer.getIdentificationCode() + ": " + e.getMessage());
                        }
                    }
                    System.out.println("📞 Contactos creados: " + contactsCreated);
                }
            }

            return new SyncResult(customersCreated, customersUpdated, errors);

        } catch (Exception e) {
            System.err.println("❌ Error fatal en sincronización: " + e.getMessage());
            throw new RuntimeException("Error en sincronización de clientes: " + e.getMessage(), e);
        }
    }

    /**
     * Construye el nombre de la tabla dinámica de carga inicial
     */
    private String buildDynamicTableName(String tenantCode, String portfolioCode, String subPortfolioCode, LoadType loadType) {
        String prefix = (loadType == LoadType.INICIAL) ? "ini_" : "";
        return String.format("%s%s_%s_%s",
                prefix,
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
    private Customer createCustomerFromRow(Map<String, Object> row, Tenant tenant, Portfolio portfolio, SubPortfolio subPortfolio) {
        Customer customer = new Customer();

        // Jerarquía completa: Inquilino -> Cartera -> Subcartera
        customer.setTenantId(tenant.getId().longValue());
        customer.setTenantName(tenant.getTenantName());
        customer.setPortfolioId(portfolio.getId().longValue());
        customer.setPortfolioName(portfolio.getPortfolioName());
        customer.setSubPortfolioId(subPortfolio.getId().longValue());
        customer.setSubPortfolioName(subPortfolio.getSubPortfolioName());

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

        Integer edad = getIntegerValue(row, "edad");
        customer.setAge(edad);
        System.out.println("   📅 Edad del row: " + edad + " (campo 'edad' en row: " + row.get("edad") + ")");

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

        // Cuenta (número de cuenta del sistema)
        customer.setAccountNumber(getStringValue(row, "numero_cuenta_linea_prestamo"));

        // Información de deuda/mora
        customer.setOverdueDays(getIntegerValue(row, "dias_mora"));
        customer.setOverdueAmount(getDoubleValue(row, "monto_mora"));
        customer.setPrincipalAmount(getDoubleValue(row, "monto_capital"));

        return customer;
    }

    /**
     * Crea un nuevo cliente desde los datos de la fila (método legacy sin portfolio/subportfolio)
     * @deprecated Usar createCustomerFromRow(row, tenant, portfolio, subPortfolio) en su lugar
     */
    @Deprecated
    private Customer createCustomerFromRowLegacy(Map<String, Object> row, Long tenantId) {
        Customer customer = new Customer();

        // Solo tenantId disponible en este método legacy
        customer.setTenantId(tenantId);
        // portfolioId, portfolioName, subPortfolioId, subPortfolioName quedarán null

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

        Integer edad = getIntegerValue(row, "edad");
        customer.setAge(edad);
        System.out.println("   📅 Edad del row: " + edad + " (campo 'edad' en row: " + row.get("edad") + ")");

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

        // Cuenta (número de cuenta del sistema)
        customer.setAccountNumber(getStringValue(row, "numero_cuenta_linea_prestamo"));

        // Información de deuda/mora
        customer.setOverdueDays(getIntegerValue(row, "dias_mora"));
        customer.setOverdueAmount(getDoubleValue(row, "monto_mora"));
        customer.setPrincipalAmount(getDoubleValue(row, "monto_capital"));

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
        customer.setAge(getIntegerValue(row, "edad"));
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

        // Actualizar cuenta (número de cuenta del sistema)
        customer.setAccountNumber(getStringValue(row, "numero_cuenta_linea_prestamo"));

        // Actualizar información de deuda/mora
        customer.setOverdueDays(getIntegerValue(row, "dias_mora"));
        customer.setOverdueAmount(getDoubleValue(row, "monto_mora"));
        customer.setPrincipalAmount(getDoubleValue(row, "monto_capital"));
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
     * Obtiene un valor Integer de la fila
     */
    private Integer getIntegerValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                String strValue = value.toString().trim();
                // Si el valor tiene decimales (ej: "71.0"), convertir primero a double
                if (strValue.contains(".")) {
                    return (int) Double.parseDouble(strValue);
                } else {
                    return Integer.parseInt(strValue);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Error parseando Integer del campo '" + columnName + "': " + value);
            return null;
        }
    }

    /**
     * Obtiene un valor Double de la fila
     */
    private Double getDoubleValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else {
                return Double.parseDouble(value.toString().trim());
            }
        } catch (NumberFormatException e) {
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
     * Mapea las columnas de la tabla dinámica (nombres de financiera) a nombres del sistema
     * usando la configuración de cabeceras
     */
    private Map<String, Object> mapColumnsToSystemFields(Map<String, Object> row, SubPortfolio subPortfolio, LoadType loadType) {
        // Obtener configuraciones de cabecera para este subportfolio
        List<HeaderConfiguration> headerConfigs = headerConfigurationRepository
                .findBySubPortfolioAndLoadType(subPortfolio, loadType);

        System.out.println("🔍 [MAPEO] Configuraciones encontradas: " + headerConfigs.size());

        // Crear mapa con columnas del sistema
        Map<String, Object> mappedRow = new HashMap<>();

        // Construir mapa de headerName -> systemFieldCode
        Map<String, String> columnMapping = new HashMap<>();
        for (HeaderConfiguration config : headerConfigs) {
            if (config.getFieldDefinition() != null) {
                String headerName = sanitizeColumnName(config.getHeaderName());
                String systemFieldCode = config.getFieldDefinition().getFieldCode();
                columnMapping.put(headerName, systemFieldCode);
                System.out.println("   ✓ Mapeo registrado: '" + config.getHeaderName() + "' (sanitized: '" + headerName + "') → '" + systemFieldCode + "' (FieldDef ID: " + config.getFieldDefinition().getId() + ")");
            } else {
                System.out.println("   ⚠️ Config sin FieldDefinition: '" + config.getHeaderName() + "' (ID config: " + config.getId() + ")");
            }
        }

        System.out.println("🔍 [MAPEO] Columnas en el row: " + row.keySet());

        // Aplicar el mapeo
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            // Si existe mapeo, usar el nombre del sistema, sino usar el nombre original
            String targetColumnName = columnMapping.getOrDefault(columnName, columnName);
            mappedRow.put(targetColumnName, value);

            // Debug log
            if (columnMapping.containsKey(columnName)) {
                System.out.println("🗺️ Mapeo aplicado: '" + columnName + "' → '" + targetColumnName + "' = " + value);
            }
        }

        // Log final para verificar si numero_cuenta_linea_prestamo está en el mappedRow
        System.out.println("🔍 [MAPEO] MappedRow contiene 'numero_cuenta_linea_prestamo': " +
                         mappedRow.containsKey("numero_cuenta_linea_prestamo"));
        if (mappedRow.containsKey("numero_cuenta_linea_prestamo")) {
            System.out.println("   💰 Valor: " + mappedRow.get("numero_cuenta_linea_prestamo"));
        }

        return mappedRow;
    }

    /**
     * Sanitiza el nombre de columna (igual que en HeaderConfigurationCommandServiceImpl)
     */
    private String sanitizeColumnName(String columnName) {
        if (columnName == null) return null;
        return columnName.toLowerCase()
                .replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("ñ", "n")
                .replaceAll("[^a-z0-9_]", "_");
    }

    /**
     * Sincroniza los contactos de un cliente desde una fila de datos
     */
    private int syncCustomerContacts(Customer customer, Map<String, Object> row) {
        int contactsCreated = 0;

        // Eliminar contactos existentes para este cliente
        contactMethodRepository.deleteByCustomerId(customer.getId());

        // Crear contactos desde los datos
        contactsCreated += createContactIfPresent(customer, "telefono_principal", "telefono", row);
        contactsCreated += createContactIfPresent(customer, "telefono_secundario", "telefono", row);
        contactsCreated += createContactIfPresent(customer, "telefono_trabajo", "telefono", row);
        contactsCreated += createContactIfPresent(customer, "email", "email", row);
        contactsCreated += createContactIfPresent(customer, "telefono_referencia_1", "telefono", row);
        contactsCreated += createContactIfPresent(customer, "telefono_referencia_2", "telefono", row);

        return contactsCreated;
    }

    /**
     * Crea un método de contacto si el valor está presente en los datos
     */
    private int createContactIfPresent(Customer customer, String subtype, String contactType, Map<String, Object> row) {
        String contactValue = getStringValue(row, subtype);
        if (contactValue != null && !contactValue.isEmpty()) {
            ContactMethod contactMethod = ContactMethod.builder()
                    .customer(customer)
                    .contactType(contactType)
                    .subtype(subtype)
                    .value(contactValue)
                    .label(subtype)
                    .importDate(LocalDate.now())
                    .status("ACTIVE")
                    .build();

            contactMethodRepository.save(contactMethod);
            return 1;
        }
        return 0;
    }

    /**
     * Guarda clientes en la tabla de prueba usando JDBC UPSERT (INSERT ... ON DUPLICATE KEY UPDATE).
     * Retorna un Map de codigo_identificacion -> id para poder asociar los contactos.
     * NO hace TRUNCATE - mantiene los datos existentes y actualiza/inserta según corresponda.
     */
    private Map<String, Long> saveCustomersToTestTableWithIds(List<Customer> customers, List<Map<String, Object>> rows) {
        Map<String, Long> resultMap = new HashMap<>();
        if (customers.isEmpty()) return resultMap;

        // UPSERT: INSERT ... ON DUPLICATE KEY UPDATE
        // Asume que 'codigo_identificacion' es UNIQUE KEY
        String upsertSql = "INSERT INTO " + testTableName + " (" +
            "id_inquilino, nombre_inquilino, id_cartera, nombre_cartera, id_subcartera, nombre_subcartera, " +
            "id_cliente, codigo_identificacion, documento, " +
            "nombre_completo, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, " +
            "fecha_nacimiento, edad, estado_civil, " +
            "ocupacion, tipo_cliente, " +
            "direccion, distrito, provincia, departamento, " +
            "referencia_personal, numero_cuenta_linea_prestamo, " +
            "dias_mora, monto_mora, monto_capital, " +
            "fecha_creacion, fecha_actualizacion" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "id_inquilino = VALUES(id_inquilino), " +
            "nombre_inquilino = VALUES(nombre_inquilino), " +
            "id_cartera = VALUES(id_cartera), " +
            "nombre_cartera = VALUES(nombre_cartera), " +
            "id_subcartera = VALUES(id_subcartera), " +
            "nombre_subcartera = VALUES(nombre_subcartera), " +
            "nombre_completo = VALUES(nombre_completo), " +
            "primer_nombre = VALUES(primer_nombre), " +
            "segundo_nombre = VALUES(segundo_nombre), " +
            "primer_apellido = VALUES(primer_apellido), " +
            "segundo_apellido = VALUES(segundo_apellido), " +
            "fecha_nacimiento = VALUES(fecha_nacimiento), " +
            "edad = VALUES(edad), " +
            "estado_civil = VALUES(estado_civil), " +
            "ocupacion = VALUES(ocupacion), " +
            "tipo_cliente = VALUES(tipo_cliente), " +
            "direccion = VALUES(direccion), " +
            "distrito = VALUES(distrito), " +
            "provincia = VALUES(provincia), " +
            "departamento = VALUES(departamento), " +
            "referencia_personal = VALUES(referencia_personal), " +
            "numero_cuenta_linea_prestamo = VALUES(numero_cuenta_linea_prestamo), " +
            "dias_mora = VALUES(dias_mora), " +
            "monto_mora = VALUES(monto_mora), " +
            "monto_capital = VALUES(monto_capital), " +
            "fecha_actualizacion = NOW()";

        List<Object[]> batchArgs = new ArrayList<>();
        List<String> identificationCodes = new ArrayList<>();

        for (Customer customer : customers) {
            Object[] args = new Object[] {
                customer.getTenantId(),
                customer.getTenantName(),
                customer.getPortfolioId(),
                customer.getPortfolioName(),
                customer.getSubPortfolioId(),
                customer.getSubPortfolioName(),
                customer.getCustomerId(),
                customer.getIdentificationCode(),
                customer.getDocument(),
                customer.getFullName(),
                customer.getFirstName(),
                customer.getSecondName(),
                customer.getFirstLastName(),
                customer.getSecondLastName(),
                customer.getBirthDate(),
                customer.getAge(),
                customer.getMaritalStatus(),
                customer.getOccupation(),
                customer.getCustomerType(),
                customer.getAddress(),
                customer.getDistrict(),
                customer.getProvince(),
                customer.getDepartment(),
                customer.getPersonalReference(),
                customer.getAccountNumber(),
                customer.getOverdueDays(),
                customer.getOverdueAmount(),
                customer.getPrincipalAmount()
            };
            batchArgs.add(args);
            identificationCodes.add(customer.getIdentificationCode());
        }

        // Ejecutar batch UPSERT
        System.out.println("🔄 Ejecutando UPSERT de " + batchArgs.size() + " clientes en tabla " + testTableName);
        int[] results = jdbcTemplate.batchUpdate(upsertSql, batchArgs);

        int totalInserted = 0;
        int totalUpdated = 0;
        for (int r : results) {
            if (r == 1) totalInserted++;
            else if (r == 2) totalUpdated++;
            else if (r == -2) totalInserted++;
        }

        System.out.println("✅ UPSERT completado: " + totalInserted + " insertados, " + totalUpdated + " actualizados");

        // Obtener los IDs de los clientes insertados/actualizados
        String selectIdsSql = "SELECT id, codigo_identificacion FROM " + testTableName +
                              " WHERE codigo_identificacion IN (" +
                              String.join(",", Collections.nCopies(identificationCodes.size(), "?")) + ")";

        List<Map<String, Object>> idResults = jdbcTemplate.queryForList(selectIdsSql, identificationCodes.toArray());
        for (Map<String, Object> row : idResults) {
            Long id = ((Number) row.get("id")).longValue();
            String code = (String) row.get("codigo_identificacion");
            resultMap.put(code, id);
        }

        return resultMap;
    }

    /**
     * Sincroniza los contactos de un cliente a la tabla de prueba
     */
    private int syncCustomerContactsToTestTable(Long clientId, Map<String, Object> row) {
        int contactsCreated = 0;

        // Primero eliminar contactos existentes para este cliente en la tabla de prueba
        jdbcTemplate.update("DELETE FROM " + testContactTableName + " WHERE id_cliente = ?", clientId);

        // Insertar nuevos contactos
        String insertSql = "INSERT INTO " + testContactTableName +
            " (id_cliente, tipo_contacto, subtipo, valor, etiqueta, fecha_importacion, estado) " +
            "VALUES (?, ?, ?, ?, ?, CURDATE(), 'ACTIVE')";

        // Crear contactos desde los datos mapeados
        contactsCreated += insertContactIfPresent(insertSql, clientId, "telefono_principal", "telefono", row);
        contactsCreated += insertContactIfPresent(insertSql, clientId, "telefono_secundario", "telefono", row);
        contactsCreated += insertContactIfPresent(insertSql, clientId, "telefono_trabajo", "telefono", row);
        contactsCreated += insertContactIfPresent(insertSql, clientId, "email", "email", row);
        contactsCreated += insertContactIfPresent(insertSql, clientId, "telefono_referencia_1", "telefono", row);
        contactsCreated += insertContactIfPresent(insertSql, clientId, "telefono_referencia_2", "telefono", row);

        return contactsCreated;
    }

    /**
     * Inserta un contacto en la tabla de prueba si el valor está presente
     */
    private int insertContactIfPresent(String insertSql, Long clientId, String subtype, String contactType, Map<String, Object> row) {
        String contactValue = getStringValue(row, subtype);
        if (contactValue != null && !contactValue.isEmpty()) {
            jdbcTemplate.update(insertSql, clientId, contactType, subtype, contactValue, subtype);
            return 1;
        }
        return 0;
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
