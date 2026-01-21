package com.cashi.customermanagement.application.internal.commandservices;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;

/**
 * Servicio para sincronizar clientes desde tablas dinámicas a tabla clientes.
 * Lee datos de tablas <prov>_<car>_<subcartera> (LoadType.INICIAL) y los consolida en la tabla clientes.
 *
 * Optimizaciones implementadas:
 * - UPSERT batch para clientes (INSERT ... ON DUPLICATE KEY UPDATE)
 * - DELETE/INSERT batch para contactos (reduce ~23000 ops a ~50 ops)
 * - Query de IDs en batches de 500 para evitar IN clause gigantes
 */
@Service
public class CustomerSyncService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerSyncService.class);

    @PersistenceContext
    private final EntityManager entityManager;

    private final CustomerRepository customerRepository;
    private final SubPortfolioRepository subPortfolioRepository;
    private final HeaderConfigurationRepository headerConfigurationRepository;
    private final JdbcTemplate jdbcTemplate;

    public CustomerSyncService(
            EntityManager entityManager,
            CustomerRepository customerRepository,
            SubPortfolioRepository subPortfolioRepository,
            HeaderConfigurationRepository headerConfigurationRepository,
            JdbcTemplate jdbcTemplate) {
        this.entityManager = entityManager;
        this.customerRepository = customerRepository;
        this.subPortfolioRepository = subPortfolioRepository;
        this.headerConfigurationRepository = headerConfigurationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Sincroniza clientes desde una tabla específica (método directo)
     */
    @Transactional
    public SyncResult syncCustomersFromTable(String tableName, Long tenantId) {
        logger.info("Iniciando sincronización de clientes desde tabla: {}", tableName);

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

            // ========== BATCH SAVE: Guardar todos los clientes con UPSERT ==========
            if (!customersToSave.isEmpty()) {
                Map<String, Long> savedIdsMap = saveCustomersToProductionTableWithUpsert(customersToSave);
                int contactsCreated = syncAllCustomerContactsBatch(savedIdsMap, customersToSave, rowsToSync, "metodos_contacto");
                logger.info("Sincronización completada: {} clientes, {} contactos", savedIdsMap.size(), contactsCreated);
            }

            return new SyncResult(customersCreated, customersUpdated, errors);

        } catch (Exception e) {
            logger.error("Error en sincronización desde tabla {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Error en sincronización de clientes: " + e.getMessage(), e);
        }
    }

    /**
     * Sincroniza clientes de una sub-cartera específica con un LoadType específico
     */
    @Transactional
    public SyncResult syncCustomersFromSubPortfolio(Long subPortfolioId, LoadType loadType) {
        logger.info("Sincronizando clientes para SubPortfolio: {}, LoadType: {}", subPortfolioId, loadType);

        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("SubPortfolio no encontrado: " + subPortfolioId));

        Portfolio portfolio = subPortfolio.getPortfolio();
        Tenant tenant = portfolio.getTenant();

        String tableName = buildDynamicTableName(
                tenant.getTenantCode(),
                portfolio.getPortfolioCode(),
                subPortfolio.getSubPortfolioCode(),
                loadType
        );

        logger.debug("Tabla dinámica: {}", tableName);

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

            // ========== BATCH SAVE: Guardar todos los clientes con UPSERT ==========
            if (!customersToSave.isEmpty()) {
                Map<String, Long> savedIdsMap = saveCustomersToProductionTableWithUpsert(customersToSave);
                int contactsCreated = syncAllCustomerContactsBatch(savedIdsMap, customersToSave, rowsToSync, "metodos_contacto");
                logger.info("Sincronización SubPortfolio {} completada: {} clientes, {} contactos",
                        subPortfolioId, savedIdsMap.size(), contactsCreated);
            }

            return new SyncResult(customersCreated, customersUpdated, errors);

        } catch (Exception e) {
            logger.error("Error en sincronización SubPortfolio {}: {}", subPortfolioId, e.getMessage(), e);
            throw new RuntimeException("Error en sincronización de clientes: " + e.getMessage(), e);
        }
    }

    /**
     * Umbral para decidir entre sincronización selectiva vs completa.
     * Si se actualizan más de este número de registros, es más eficiente cargar todo.
     * NOTA: Con las optimizaciones batch, este umbral se puede aumentar significativamente.
     * Valor anterior: 500 (causaba fallback innecesario en cargas de ~6000 registros)
     */
    private static final int SELECTIVE_SYNC_THRESHOLD = 10000;

    /**
     * Sincroniza SOLO los clientes especificados por sus códigos de identificación.
     * Usado para carga diaria donde solo queremos sincronizar los registros que fueron actualizados.
     *
     * OPTIMIZACIÓN: Si hay más de SELECTIVE_SYNC_THRESHOLD códigos, usa el método completo
     * porque el overhead del IN clause grande es mayor que cargar todo.
     *
     * @param subPortfolioId ID de la subcartera
     * @param loadType Tipo de carga (normalmente INICIAL)
     * @param identificationCodes Lista de códigos de identificación a sincronizar
     * @return Resultado de la sincronización
     */
    @Transactional
    public SyncResult syncCustomersByIdentificationCodes(Long subPortfolioId, LoadType loadType, Set<String> identificationCodes) {
        if (identificationCodes == null || identificationCodes.isEmpty()) {
            logger.debug("No hay códigos de identificación para sincronizar");
            return new SyncResult(0, 0, new ArrayList<>());
        }

        // Si hay muchos códigos, es más eficiente usar el método completo
        if (identificationCodes.size() > SELECTIVE_SYNC_THRESHOLD) {
            logger.info("Sincronización completa: {} códigos superan umbral de {}",
                    identificationCodes.size(), SELECTIVE_SYNC_THRESHOLD);
            return syncCustomersFromSubPortfolio(subPortfolioId, loadType);
        }

        logger.info("Sincronización selectiva: {} clientes para SubPortfolio {}",
                identificationCodes.size(), subPortfolioId);

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

        logger.debug("Tabla dinámica: {}", tableName);

        int customersCreated = 0;
        int customersUpdated = 0;
        List<String> errors = new ArrayList<>();

        try {
            if (!tableExists(tableName)) {
                throw new IllegalArgumentException("La tabla dinámica no existe: " + tableName);
            }

            List<Map<String, Object>> rows = readDynamicTableDataByIds(tableName, identificationCodes, subPortfolio, loadType);
            logger.debug("Registros encontrados: {}", rows.size());

            if (rows.isEmpty()) {
                logger.debug("No se encontraron registros con los códigos especificados");
                return new SyncResult(0, 0, errors);
            }

            Map<String, Customer> existingCustomersMap = loadExistingCustomersByIds(tenant.getId().longValue(), identificationCodes);
            logger.debug("Clientes existentes: {} de {}", existingCustomersMap.size(), identificationCodes.size());

            // Listas para batch operations
            List<Customer> customersToSave = new ArrayList<>();
            List<Map<String, Object>> rowsToSync = new ArrayList<>();

            // 6. Procesar cada registro
            for (Map<String, Object> row : rows) {
                try {
                    Map<String, Object> mappedRow = mapColumnsToSystemFields(row, subPortfolio, loadType);

                    String identificationCode = getStringValue(mappedRow, "codigo_identificacion");
                    String document = getStringValue(mappedRow, "documento");

                    if (document == null || document.isEmpty()) {
                        errors.add("Documento vacío en registro");
                        continue;
                    }

                    Customer existingCustomer = existingCustomersMap.get(identificationCode);

                    Customer customer;
                    if (existingCustomer != null) {
                        customer = existingCustomer;
                        customer.setTenantId(tenant.getId().longValue());
                        customer.setTenantName(tenant.getTenantName());
                        customer.setPortfolioId(portfolio.getId().longValue());
                        customer.setPortfolioName(portfolio.getPortfolioName());
                        customer.setSubPortfolioId(subPortfolioId.longValue());
                        customer.setSubPortfolioName(subPortfolio.getSubPortfolioName());
                        updateCustomerFromRow(customer, mappedRow);
                        customersUpdated++;
                    } else {
                        customer = createCustomerFromRow(mappedRow, tenant, portfolio, subPortfolio);
                        customersCreated++;
                    }

                    customersToSave.add(customer);
                    rowsToSync.add(mappedRow);

                } catch (Exception e) {
                    errors.add("Error procesando registro: " + e.getMessage());
                }
            }

            // 7. BATCH SAVE con UPSERT
            if (!customersToSave.isEmpty()) {
                Map<String, Long> savedIdsMap = saveCustomersToProductionTableWithUpsert(customersToSave);
                int contactsCreated = syncAllCustomerContactsBatch(savedIdsMap, customersToSave, rowsToSync, "metodos_contacto");
                logger.info("Sincronización selectiva completada: {} clientes, {} contactos",
                        savedIdsMap.size(), contactsCreated);
            }

            return new SyncResult(customersCreated, customersUpdated, errors);

        } catch (Exception e) {
            logger.error("Error en sincronización selectiva: {}", e.getMessage(), e);
            throw new RuntimeException("Error en sincronización de clientes: " + e.getMessage(), e);
        }
    }

    /**
     * Carga solo los clientes específicos por sus códigos de identificación (optimizado).
     * En lugar de cargar todos los clientes del tenant, carga solo los que necesitamos.
     */
    private Map<String, Customer> loadExistingCustomersByIds(Long tenantId, Set<String> identificationCodes) {
        Map<String, Customer> resultMap = new HashMap<>();
        if (identificationCodes.isEmpty()) {
            return resultMap;
        }

        // Usar el repository para buscar por tenant y códigos específicos
        List<Customer> customers = customerRepository.findByTenantIdAndIdentificationCodeIn(tenantId, identificationCodes);

        for (Customer c : customers) {
            if (c.getIdentificationCode() != null) {
                resultMap.put(c.getIdentificationCode(), c);
            }
        }

        return resultMap;
    }

    /**
     * Lee datos de la tabla dinámica filtrando solo por los códigos de identificación especificados.
     *
     * IMPORTANTE: Las tablas dinámicas usan los nombres originales del Excel, no los nombres del sistema.
     * Este método consulta HeaderConfiguration para encontrar el nombre real de la columna que mapea
     * a 'codigo_identificacion' del sistema.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readDynamicTableDataByIds(String tableName, Set<String> identificationCodes,
                                                                  SubPortfolio subPortfolio, LoadType loadType) {
        if (identificationCodes.isEmpty()) {
            return new ArrayList<>();
        }

        // Buscar el nombre real de la columna que mapea a 'codigo_identificacion'
        String actualColumnName = findActualColumnName(subPortfolio, loadType, "codigo_identificacion");

        if (actualColumnName == null) {
            logger.warn("No se encontró configuración de cabecera para 'codigo_identificacion' en subportfolio {}, loadType {}",
                    subPortfolio.getId(), loadType);
            actualColumnName = "codigo_identificacion";
        }

        logger.debug("Usando columna '{}' para filtrar por codigo_identificacion", actualColumnName);

        // Construir placeholders para IN clause
        String placeholders = String.join(",", Collections.nCopies(identificationCodes.size(), "?"));
        String sql = "SELECT * FROM " + tableName + " WHERE `" + actualColumnName + "` IN (" + placeholders + ")";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, identificationCodes.toArray());
        return results;
    }

    /**
     * Busca el nombre real de la columna en la tabla dinámica que corresponde a un campo del sistema.
     *
     * @param subPortfolio SubPortfolio donde buscar la configuración
     * @param loadType Tipo de carga (INICIAL, ACTUALIZACION, etc.)
     * @param systemFieldCode Código del campo del sistema (ej: 'codigo_identificacion')
     * @return El nombre sanitizado de la columna en la tabla dinámica, o null si no se encuentra
     */
    private String findActualColumnName(SubPortfolio subPortfolio, LoadType loadType, String systemFieldCode) {
        List<HeaderConfiguration> headerConfigs = headerConfigurationRepository
                .findBySubPortfolioAndLoadType(subPortfolio, loadType);

        for (HeaderConfiguration config : headerConfigs) {
            if (config.getFieldDefinition() != null &&
                systemFieldCode.equals(config.getFieldDefinition().getFieldCode())) {
                // Retornar el nombre sanitizado de la columna (como se guarda en la tabla dinámica)
                return sanitizeColumnName(config.getHeaderName());
            }
        }

        return null;
    }

    /**
     * Construye el nombre de la tabla dinámica según el tipo de carga
     */
    private String buildDynamicTableName(String tenantCode, String portfolioCode, String subPortfolioCode, LoadType loadType) {
        String baseName = String.format("%s_%s_%s",
                tenantCode.toLowerCase(),
                portfolioCode.toLowerCase(),
                subPortfolioCode.toLowerCase()
        );
        // Usar el prefijo definido en LoadType
        return loadType.getTablePrefix() + baseName;
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
            logger.debug("Error parseando fecha: {}", value);
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
            logger.debug("Error parseando Integer del campo '{}': {}", columnName, value);
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

        // Crear mapa con columnas del sistema
        Map<String, Object> mappedRow = new HashMap<>();

        // Construir mapa de headerName -> systemFieldCode
        Map<String, String> columnMapping = new HashMap<>();
        for (HeaderConfiguration config : headerConfigs) {
            if (config.getFieldDefinition() != null) {
                String headerName = sanitizeColumnName(config.getHeaderName());
                String systemFieldCode = config.getFieldDefinition().getFieldCode();
                columnMapping.put(headerName, systemFieldCode);
            }
        }

        // Aplicar el mapeo
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();
            String targetColumnName = columnMapping.getOrDefault(columnName, columnName);
            mappedRow.put(targetColumnName, value);
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
     * Guarda clientes en la tabla de producción 'clientes' usando JDBC UPSERT (INSERT ... ON DUPLICATE KEY UPDATE).
     * Retorna un Map de codigo_identificacion -> id para poder asociar los contactos.
     */
    private Map<String, Long> saveCustomersToProductionTableWithUpsert(List<Customer> customers) {
        Map<String, Long> resultMap = new HashMap<>();
        if (customers.isEmpty()) return resultMap;

        // UPSERT: INSERT ... ON DUPLICATE KEY UPDATE
        // Asume que 'codigo_identificacion' es UNIQUE KEY en tabla 'clientes'
        String upsertSql = "INSERT INTO clientes (" +
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
        List<String> lookupKeys = new ArrayList<>();

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

            // Usar codigo_identificacion si existe, sino documento
            String lookupKey = customer.getIdentificationCode();
            if (lookupKey == null || lookupKey.isEmpty()) {
                lookupKey = customer.getDocument();
            }
            lookupKeys.add(lookupKey);
        }

        // Ejecutar batch UPSERT
        logger.debug("Ejecutando UPSERT de {} clientes", batchArgs.size());
        int[] results = jdbcTemplate.batchUpdate(upsertSql, batchArgs);

        int totalInserted = 0;
        int totalUpdated = 0;
        for (int r : results) {
            if (r == 1) totalInserted++;
            else if (r == 2) totalUpdated++;
            else if (r == -2) totalInserted++;
        }

        logger.debug("UPSERT completado: {} insertados, {} actualizados", totalInserted, totalUpdated);

        // Obtener los IDs de los clientes insertados/actualizados EN BATCHES
        // Evitar IN clause gigante que causa problemas de rendimiento
        int batchSize = 500;
        for (int i = 0; i < lookupKeys.size(); i += batchSize) {
            int end = Math.min(i + batchSize, lookupKeys.size());
            List<String> batch = lookupKeys.subList(i, end);

            String placeholders = String.join(",", Collections.nCopies(batch.size(), "?"));
            String selectIdsSql = "SELECT id, codigo_identificacion, documento FROM clientes " +
                                  "WHERE codigo_identificacion IN (" + placeholders + ")";

            List<Map<String, Object>> idResults = jdbcTemplate.queryForList(selectIdsSql, batch.toArray());
            for (Map<String, Object> row : idResults) {
                Long id = ((Number) row.get("id")).longValue();
                String code = (String) row.get("codigo_identificacion");
                String documento = (String) row.get("documento");

                if (code != null && !code.isEmpty()) {
                    resultMap.put(code, id);
                }
                if (documento != null && !documento.isEmpty()) {
                    resultMap.put(documento, id);
                }
            }
        }

        logger.debug("IDs recuperados: {} claves mapeadas", resultMap.size());
        return resultMap;
    }

    /**
     * Sincroniza los contactos de un cliente a la tabla de producción metodos_contacto con UPSERT
     * NOTA: Este método es para uso individual. Para batch, usar syncAllCustomerContactsBatch()
     */
    private int syncCustomerContactsWithUpsert(Long clientId, Map<String, Object> row) {
        int contactsCreated = 0;

        // Primero eliminar contactos existentes para este cliente
        jdbcTemplate.update("DELETE FROM metodos_contacto WHERE id_cliente = ?", clientId);

        // Insertar nuevos contactos
        String insertSql = "INSERT INTO metodos_contacto " +
            "(id_cliente, tipo_contacto, subtipo, valor, etiqueta, fecha_importacion, estado) " +
            "VALUES (?, ?, ?, ?, ?, CURDATE(), 'ACTIVE')";

        // Crear contactos desde los datos mapeados
        contactsCreated += insertContactToProduction(insertSql, clientId, "telefono_principal", "telefono", row);
        contactsCreated += insertContactToProduction(insertSql, clientId, "telefono_secundario", "telefono", row);
        contactsCreated += insertContactToProduction(insertSql, clientId, "telefono_trabajo", "telefono", row);
        contactsCreated += insertContactToProduction(insertSql, clientId, "email", "email", row);
        contactsCreated += insertContactToProduction(insertSql, clientId, "telefono_referencia_1", "telefono", row);
        contactsCreated += insertContactToProduction(insertSql, clientId, "telefono_referencia_2", "telefono", row);

        return contactsCreated;
    }

    /**
     * OPTIMIZACIÓN: Sincroniza contactos de TODOS los clientes en batch.
     * En lugar de hacer 6390 DELETE + 17000 INSERT individuales,
     * hace DELETE en batches de 500 IDs + INSERT en batches de 1000 contactos.
     *
     * Reduce ~23000 operaciones individuales a ~50 operaciones batch.
     */
    private int syncAllCustomerContactsBatch(Map<String, Long> clientIdsMap, List<Customer> customers,
                                              List<Map<String, Object>> rows, String tableName) {
        if (clientIdsMap.isEmpty() || customers.isEmpty()) {
            return 0;
        }

        logger.debug("Sincronizando contactos batch para {} clientes", customers.size());

        // 1. Recopilar todos los IDs de clientes para DELETE batch
        Set<Long> allClientIds = new HashSet<>();
        for (int i = 0; i < customers.size(); i++) {
            Customer customer = customers.get(i);
            String lookupKey = customer.getIdentificationCode();
            if (lookupKey == null || lookupKey.isEmpty()) {
                lookupKey = customer.getDocument();
            }
            Long clientId = clientIdsMap.get(lookupKey);
            if (clientId != null) {
                allClientIds.add(clientId);
            }
        }

        // 2. DELETE en batches de 500 IDs
        List<Long> clientIdList = new ArrayList<>(allClientIds);
        int deleteBatchSize = 500;
        int totalDeleted = 0;

        for (int i = 0; i < clientIdList.size(); i += deleteBatchSize) {
            int end = Math.min(i + deleteBatchSize, clientIdList.size());
            List<Long> batch = clientIdList.subList(i, end);

            String placeholders = String.join(",", Collections.nCopies(batch.size(), "?"));
            String deleteSql = "DELETE FROM " + tableName + " WHERE id_cliente IN (" + placeholders + ")";

            int deleted = jdbcTemplate.update(deleteSql, batch.toArray());
            totalDeleted += deleted;
        }
        logger.debug("Eliminados {} contactos existentes", totalDeleted);

        // 3. Recopilar todos los contactos a insertar
        List<Object[]> contactsToInsert = new ArrayList<>();
        String[] contactSubtypes = {"telefono_principal", "telefono_secundario", "telefono_trabajo",
                                    "email", "telefono_referencia_1", "telefono_referencia_2"};

        for (int i = 0; i < customers.size(); i++) {
            Customer customer = customers.get(i);
            Map<String, Object> row = rows.get(i);

            String lookupKey = customer.getIdentificationCode();
            if (lookupKey == null || lookupKey.isEmpty()) {
                lookupKey = customer.getDocument();
            }
            Long clientId = clientIdsMap.get(lookupKey);

            if (clientId != null) {
                for (String subtype : contactSubtypes) {
                    String contactValue = getStringValue(row, subtype);
                    if (contactValue != null && !contactValue.isEmpty()) {
                        String contactType = subtype.equals("email") ? "email" : "telefono";
                        contactsToInsert.add(new Object[]{
                            clientId, contactType, subtype, contactValue, subtype
                        });
                    }
                }
            }
        }

        // 4. INSERT en batches de 1000 contactos
        if (contactsToInsert.isEmpty()) {
            logger.debug("No hay contactos para insertar");
            return 0;
        }

        String insertSql = "INSERT INTO " + tableName +
            " (id_cliente, tipo_contacto, subtipo, valor, etiqueta, fecha_importacion, estado) " +
            "VALUES (?, ?, ?, ?, ?, CURDATE(), 'ACTIVE')";

        int insertBatchSize = 1000;
        int totalInserted = 0;

        for (int i = 0; i < contactsToInsert.size(); i += insertBatchSize) {
            int end = Math.min(i + insertBatchSize, contactsToInsert.size());
            List<Object[]> batch = contactsToInsert.subList(i, end);

            int[] results = jdbcTemplate.batchUpdate(insertSql, batch);
            for (int r : results) {
                if (r > 0 || r == -2) totalInserted++;
            }
        }

        logger.debug("Insertados {} contactos", totalInserted);
        return totalInserted;
    }

    /**
     * Inserta un contacto en la tabla de producción si el valor está presente
     */
    private int insertContactToProduction(String insertSql, Long clientId, String subtype, String contactType, Map<String, Object> row) {
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
