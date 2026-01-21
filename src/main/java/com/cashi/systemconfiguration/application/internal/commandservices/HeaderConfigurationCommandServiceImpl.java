package com.cashi.systemconfiguration.application.internal.commandservices;

import com.cashi.customermanagement.application.internal.commandservices.CustomerSyncService;
import com.cashi.customermanagement.domain.model.aggregates.Customer;
import com.cashi.customermanagement.domain.model.entities.ContactMethod;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ContactMethodRepository;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerRepository;
import com.cashi.shared.domain.model.entities.FieldDefinition;
import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.FieldDefinitionRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.HeaderConfigurationRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
import com.cashi.shared.util.DataTypeValidator;
import com.cashi.shared.util.DateParserUtil;
import com.cashi.shared.util.SqlSanitizer;
import com.cashi.systemconfiguration.domain.services.HeaderConfigurationCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
public class HeaderConfigurationCommandServiceImpl implements HeaderConfigurationCommandService {

    private static final Logger logger = LoggerFactory.getLogger(HeaderConfigurationCommandServiceImpl.class);

    private final HeaderConfigurationRepository headerConfigurationRepository;
    private final SubPortfolioRepository subPortfolioRepository;
    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final CustomerRepository customerRepository;
    private final ContactMethodRepository contactMethodRepository;
    private final CustomerSyncService customerSyncService;
    private final TransactionTemplate transactionTemplate;

    // Tamaño del batch para operaciones de carga de identificadores (previene problemas de memoria)
    private static final int BATCH_SIZE_FOR_IDENTIFIER_LOAD = 10000;

    // Tamaño del batch para operaciones de UPDATE en carga diaria (optimización de rendimiento)
    private static final int BATCH_SIZE_FOR_DAILY_UPDATE = 500;

    // Tamaño del batch para operaciones de INSERT masivo
    private static final int BATCH_SIZE_FOR_INSERT = 1000;

    public HeaderConfigurationCommandServiceImpl(
            HeaderConfigurationRepository headerConfigurationRepository,
            SubPortfolioRepository subPortfolioRepository,
            FieldDefinitionRepository fieldDefinitionRepository,
            JdbcTemplate jdbcTemplate,
            CustomerRepository customerRepository,
            ContactMethodRepository contactMethodRepository,
            CustomerSyncService customerSyncService,
            TransactionTemplate transactionTemplate) {
        this.headerConfigurationRepository = headerConfigurationRepository;
        this.subPortfolioRepository = subPortfolioRepository;
        this.fieldDefinitionRepository = fieldDefinitionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.customerRepository = customerRepository;
        this.contactMethodRepository = contactMethodRepository;
        this.customerSyncService = customerSyncService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    @Transactional
    public HeaderConfiguration createHeaderConfiguration(Integer subPortfolioId, Integer fieldDefinitionId,
                                                         String headerName, String dataType, String displayLabel,
                                                         String format, Boolean required, LoadType loadType,
                                                         String sourceField, String regexPattern) {
        // Validaciones de parámetros obligatorios
        if (subPortfolioId == null) {
            throw new IllegalArgumentException("El ID de subcartera es obligatorio");
        }
        if (headerName == null || headerName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de cabecera es obligatorio");
        }
        if (loadType == null) {
            throw new IllegalArgumentException("El tipo de carga es obligatorio");
        }

        // Validar que la subcartera existe
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        String tableName = buildTableName(subPortfolio, loadType);

        // Validar que el nombre de cabecera no exista para esta subcartera y tipo de carga
        if (headerConfigurationRepository.existsBySubPortfolioAndHeaderNameAndLoadType(subPortfolio, headerName, loadType)) {
            throw new IllegalArgumentException("Ya existe una cabecera con el nombre: " + headerName + " para esta subcartera y tipo de carga");
        }

        HeaderConfiguration headerConfig;

        // Si fieldDefinitionId es null o 0, es un campo personalizado
        if (fieldDefinitionId == null || fieldDefinitionId == 0) {
            // Campo personalizado - requiere dataType
            if (dataType == null || dataType.isBlank()) {
                throw new IllegalArgumentException("El tipo de dato (dataType) es obligatorio para campos personalizados");
            }

            // Validar que dataType sea válido
            if (!List.of("TEXTO", "NUMERICO", "FECHA", "BOOLEANO").contains(dataType.toUpperCase())) {
                throw new IllegalArgumentException("DataType inválido: " + dataType + ". Valores válidos: TEXTO, NUMERICO, FECHA, BOOLEANO");
            }

            // Crear configuración de campo personalizado
            headerConfig = new HeaderConfiguration(
                    subPortfolio, headerName, dataType.toUpperCase(),
                    displayLabel, format,
                    required != null ? (required ? 1 : 0) : 0, loadType
            );
        } else {
            // Campo vinculado al catálogo
            FieldDefinition fieldDefinition = fieldDefinitionRepository.findById(fieldDefinitionId)
                    .orElseThrow(() -> new IllegalArgumentException("Definición de campo no encontrada con ID: " + fieldDefinitionId));

            // Crear configuración vinculada al catálogo
            headerConfig = new HeaderConfiguration(
                    subPortfolio, fieldDefinition, headerName, displayLabel, format,
                    required != null ? (required ? 1 : 0) : 0, loadType
            );
        }

        // Setear campos de transformación si existen
        if (sourceField != null && !sourceField.isBlank()) {
            headerConfig.setSourceField(sourceField);
        }
        if (regexPattern != null && !regexPattern.isBlank()) {
            headerConfig.setRegexPattern(regexPattern);
        }

        HeaderConfiguration saved = headerConfigurationRepository.save(headerConfig);

        // Si la tabla existe, agregar la columna (incluso si tiene datos, la columna se agrega con valores NULL)
        if (dynamicTableExists(tableName)) {
            addColumnToTable(tableName, saved);
            logger.info("Columna '{}' agregada a tabla '{}' (registros existentes tendrán valor NULL)",
                       sanitizeColumnName(headerName), tableName);
        } else {
            logger.warn("La tabla '{}' no existe. La columna se creará cuando se cree la tabla.", tableName);
        }

        return saved;
    }

    @Override
    @Transactional
    public HeaderConfiguration updateHeaderConfiguration(Integer id, String displayLabel,
                                                         String format, Boolean required, LoadType loadType) {
        HeaderConfiguration headerConfig = headerConfigurationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuración de cabecera no encontrada con ID: " + id));

        // Actualizar campos opcionales
        if (displayLabel != null && !displayLabel.isBlank()) {
            headerConfig.setDisplayLabel(displayLabel);
        }

        if (format != null) {
            headerConfig.setFormat(format);
        }

        if (required != null) {
            headerConfig.setRequired(required ? 1 : 0);
        }

        if (loadType != null) {
            headerConfig.setLoadType(loadType);
        }

        return headerConfigurationRepository.save(headerConfig);
    }

    @Override
    @Transactional
    public void deleteHeaderConfiguration(Integer id) {
        HeaderConfiguration headerConfig = headerConfigurationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuración de cabecera no encontrada con ID: " + id));

        SubPortfolio subPortfolio = headerConfig.getSubPortfolio();
        LoadType loadType = headerConfig.getLoadType();
        String tableName = buildTableName(subPortfolio, loadType);
        String columnName = sanitizeColumnName(headerConfig.getHeaderName());

        // Verificar si la COLUMNA ESPECÍFICA tiene datos no-NULL
        // Solo bloquear eliminación si la columna tiene valores reales, no si solo tiene NULLs
        if (dynamicTableExists(tableName) && columnHasNonNullData(tableName, columnName)) {
            throw new IllegalArgumentException(
                "No se puede eliminar la cabecera '" + headerConfig.getHeaderName() +
                "' porque la columna contiene datos. Debe eliminar los datos primero."
            );
        }

        // Eliminar la columna de la tabla si existe
        if (dynamicTableExists(tableName)) {
            dropColumnFromTable(tableName, headerConfig);
            logger.info("Columna '{}' eliminada de tabla '{}'", columnName, tableName);
        }

        headerConfigurationRepository.delete(headerConfig);
        logger.info("Configuración de cabecera eliminada: ID={}, nombre='{}'", id, headerConfig.getHeaderName());
    }

    @Override
    @Transactional
    public List<HeaderConfiguration> createBulkHeaderConfigurations(Integer subPortfolioId,
                                                                    LoadType loadType,
                                                                    List<HeaderConfigurationData> headers) {
        // Validar que la subcartera existe
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        String tableName = buildTableName(subPortfolio, loadType);
        boolean tableExists = dynamicTableExists(tableName);

        List<HeaderConfiguration> createdConfigs = new ArrayList<>();

        for (HeaderConfigurationData data : headers) {
            // Validar que el nombre no exista para esta subcartera y tipo de carga
            if (headerConfigurationRepository.existsBySubPortfolioAndHeaderNameAndLoadType(subPortfolio, data.headerName(), loadType)) {
                throw new IllegalArgumentException("Ya existe una cabecera con el nombre: " + data.headerName() + " para este tipo de carga");
            }

            HeaderConfiguration headerConfig;

            // Si fieldDefinitionId es 0 o null, es un campo personalizado
            if (data.fieldDefinitionId() == null || data.fieldDefinitionId() == 0) {
                // Campo personalizado - requiere dataType
                if (data.dataType() == null || data.dataType().isBlank()) {
                    throw new IllegalArgumentException("El campo personalizado '" + data.headerName() + "' requiere especificar dataType");
                }

                // Validar que dataType sea válido
                if (!List.of("TEXTO", "NUMERICO", "FECHA", "BOOLEANO").contains(data.dataType().toUpperCase())) {
                    throw new IllegalArgumentException("DataType inválido para campo personalizado: " + data.dataType() + ". Valores válidos: TEXTO, NUMERICO, FECHA, BOOLEANO");
                }

                // Crear configuración de campo personalizado
                headerConfig = new HeaderConfiguration(
                        subPortfolio, data.headerName(), data.dataType().toUpperCase(),
                        data.displayLabel(), data.format(),
                        data.required() != null ? (data.required() ? 1 : 0) : 0, loadType
                );
            } else {
                // Campo vinculado al catálogo
                FieldDefinition fieldDefinition = fieldDefinitionRepository.findById(data.fieldDefinitionId())
                        .orElseThrow(() -> new IllegalArgumentException("Definición de campo no encontrada con ID: " + data.fieldDefinitionId()));

                // Crear configuración vinculada al catálogo
                headerConfig = new HeaderConfiguration(
                        subPortfolio, fieldDefinition, data.headerName(),
                        data.displayLabel(), data.format(),
                        data.required() != null ? (data.required() ? 1 : 0) : 0, loadType
                );
            }

            // Setear campos de transformación
            if (data.sourceField() != null && !data.sourceField().isBlank()) {
                headerConfig.setSourceField(data.sourceField());
            }
            if (data.regexPattern() != null && !data.regexPattern().isBlank()) {
                headerConfig.setRegexPattern(data.regexPattern());
            }

            HeaderConfiguration saved = headerConfigurationRepository.save(headerConfig);
            createdConfigs.add(saved);

            // Si la tabla ya existe, agregar la columna individualmente
            if (tableExists) {
                addColumnToTable(tableName, saved);
                logger.info("Columna '{}' agregada a tabla existente '{}'",
                           sanitizeColumnName(data.headerName()), tableName);
            }
        }

        // Si la tabla NO existía, crearla con todas las columnas
        if (!tableExists) {
            createDynamicTableForSubPortfolio(subPortfolio, loadType, createdConfigs);
        }

        return createdConfigs;
    }

    /**
     * Crea una tabla dinámica con el formato inq_car_sub basada en las configuraciones de cabecera
     */
    private void createDynamicTableForSubPortfolio(SubPortfolio subPortfolio, LoadType loadType, List<HeaderConfiguration> headers) {
        String tableName = buildTableName(subPortfolio, loadType);

        logger.info("Creando tabla dinámica: {} (Tipo de carga: {})", tableName, loadType.getDisplayName());

        // Construir DDL para crear la tabla
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");
        ddl.append("  id INTEGER NOT NULL AUTO_INCREMENT,\n");

        // Agregar columnas dinámicas basadas en las configuraciones
        for (HeaderConfiguration header : headers) {
            String columnName = sanitizeColumnName(header.getHeaderName());
            String dataType = header.getDataType();
            String format = header.getFormat();
            String sqlType = mapDataTypeToSQL(dataType, format);

            ddl.append("  ").append(columnName).append(" ").append(sqlType).append(",\n");
        }

        ddl.append("  PRIMARY KEY (id)\n");
        ddl.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        try {
            jdbcTemplate.execute(ddl.toString());
            logger.info("Tabla {} creada exitosamente con {} columnas", tableName, headers.size());
        } catch (Exception e) {
            logger.error("Error al crear tabla {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Error al crear tabla dinámica: " + tableName, e);
        }
    }

    /**
     * Sanitiza el nombre de la columna eliminando caracteres especiales
     * Usa SqlSanitizer para prevenir SQL injection
     */
    private String sanitizeColumnName(String headerName) {
        return SqlSanitizer.headerToColumnName(headerName);
    }

    /**
     * Valida y sanitiza un nombre de tabla
     * @throws IllegalArgumentException si el nombre es inválido
     */
    private String validateAndSanitizeTableName(String tableName) {
        return SqlSanitizer.sanitizeTableName(tableName);
    }

    /**
     * Mapea el tipo de dato de FieldDefinition a tipo SQL
     * Si se proporciona un formato personalizado, lo valida y lo usa
     * Si no hay formato, usa valores por defecto
     */
    private String mapDataTypeToSQL(String dataType, String format) {
        String dataTypeUpper = dataType.toUpperCase();

        // Si no hay formato personalizado, usar valores por defecto
        if (format == null || format.trim().isEmpty()) {
            return switch (dataTypeUpper) {
                case "TEXTO" -> "VARCHAR(255)";
                case "NUMERICO" -> "DECIMAL(18,2)";
                case "FECHA" -> "DATE";
                case "BOOLEANO", "BOOLEAN" -> "BIT";
                default -> "VARCHAR(255)";
            };
        }

        // Validar y usar formato personalizado
        String formatUpper = format.trim().toUpperCase();

        switch (dataTypeUpper) {
            case "TEXTO":
                // Para TEXTO, el formato debe ser VARCHAR(n) o TEXT
                if (formatUpper.matches("VARCHAR\\(\\d+\\)") || formatUpper.equals("TEXT") ||
                    formatUpper.equals("MEDIUMTEXT") || formatUpper.equals("LONGTEXT")) {
                    return formatUpper;
                }
                throw new IllegalArgumentException(
                    "Formato inválido para tipo TEXTO: '" + format + "'. " +
                    "Formatos válidos: VARCHAR(n), TEXT, MEDIUMTEXT, LONGTEXT"
                );

            case "NUMERICO":
                // Para NUMERICO, el formato debe ser un tipo numérico SQL válido
                if (formatUpper.matches("INT(EGER)?") ||
                    formatUpper.matches("TINYINT") ||
                    formatUpper.matches("SMALLINT") ||
                    formatUpper.matches("MEDIUMINT") ||
                    formatUpper.matches("BIGINT") ||
                    formatUpper.matches("DECIMAL\\(\\d+,\\d+\\)") ||
                    formatUpper.matches("NUMERIC\\(\\d+,\\d+\\)") ||
                    formatUpper.matches("FLOAT") ||
                    formatUpper.matches("DOUBLE")) {
                    return formatUpper;
                }
                throw new IllegalArgumentException(
                    "Formato inválido para tipo NUMERICO: '" + format + "'. " +
                    "Formatos válidos: INT, TINYINT, SMALLINT, MEDIUMINT, BIGINT, DECIMAL(p,s), NUMERIC(p,s), FLOAT, DOUBLE"
                );

            case "FECHA":
                // Para FECHA, el formato NO debe ser un tipo SQL, sino un patrón de fecha
                // El tipo SQL siempre será DATE, DATETIME o TIMESTAMP
                // Si el formato parece un tipo SQL de fecha, usarlo
                if (formatUpper.equals("DATE") || formatUpper.equals("DATETIME") ||
                    formatUpper.matches("DATETIME\\(\\d+\\)") || formatUpper.equals("TIMESTAMP") ||
                    formatUpper.matches("TIMESTAMP\\(\\d+\\)")) {
                    return formatUpper;
                }
                // Si el formato parece un patrón de fecha (dd/MM/yyyy, etc.), usar DATE por defecto
                // y el formato se usará solo para parsear
                if (formatUpper.contains("DD") || formatUpper.contains("MM") ||
                    formatUpper.contains("YYYY") || formatUpper.contains("HH") ||
                    formatUpper.contains("/") || formatUpper.contains("-")) {
                    return "DATE"; // Patrón de fecha, no tipo SQL
                }
                throw new IllegalArgumentException(
                    "Formato inválido para tipo FECHA: '" + format + "'. " +
                    "Formatos válidos: DATE, DATETIME, DATETIME(n), TIMESTAMP, TIMESTAMP(n) o patrones de fecha como 'dd/MM/yyyy'"
                );

            case "BOOLEANO":
            case "BOOLEAN":
                // Para BOOLEANO, el formato puede ser BIT o TINYINT
                if (formatUpper.equals("BIT") || formatUpper.equals("TINYINT") ||
                    formatUpper.equals("BOOLEAN")) {
                    return formatUpper.equals("BOOLEAN") ? "BIT" : formatUpper;
                }
                return "BIT"; // Default para booleano

            default:
                return "VARCHAR(255)";
        }
    }

    @Override
    @Transactional
    public void deleteAllBySubPortfolioAndLoadType(Integer subPortfolioId, LoadType loadType) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        String tableName = buildTableName(subPortfolio, loadType);

        // Verificar si la tabla tiene datos reales (no solo NULLs)
        // Solo bloquear si hay datos reales en la tabla
        if (dynamicTableExists(tableName) && tableHasAnyNonNullData(tableName)) {
            throw new IllegalArgumentException(
                "No se pueden eliminar las configuraciones porque la tabla '" + tableName +
                "' ya contiene datos. Debe eliminar los datos primero."
            );
        }

        // Eliminar la tabla dinámica completa si existe
        if (dynamicTableExists(tableName)) {
            dropDynamicTable(tableName);
            logger.info("Tabla dinámica '{}' eliminada", tableName);
        }

        headerConfigurationRepository.deleteBySubPortfolioAndLoadType(subPortfolio, loadType);
        logger.info("Configuraciones de cabeceras eliminadas para subPortfolioId={}, loadType={}",
                   subPortfolioId, loadType);
    }

    /**
     * Construye el nombre de la tabla dinámica basado en la subcartera
     * @deprecated Use buildTableName(SubPortfolio, LoadType) instead
     */
    @Deprecated
    private String buildTableName(SubPortfolio subPortfolio) {
        // Default to ACTUALIZACION for backward compatibility
        return buildTableName(subPortfolio, LoadType.ACTUALIZACION);
    }

    /**
     * Construye el nombre de la tabla dinámica basado en la subcartera y el tipo de carga
     * INICIAL: <codproveedor>_<codcartera>_<codsubcartera> (sin prefijo) - tabla maestra de trabajo
     * ACTUALIZACION: ini_<codproveedor>_<codcartera>_<codsubcartera> - histórico diario
     */
    private String buildTableName(SubPortfolio subPortfolio, LoadType loadType) {
        String tenantCode = subPortfolio.getPortfolio().getTenant().getTenantCode().toLowerCase();
        String portfolioCode = subPortfolio.getPortfolio().getPortfolioCode().toLowerCase();
        String subPortfolioCode = subPortfolio.getSubPortfolioCode().toLowerCase();
        String baseName = tenantCode + "_" + portfolioCode + "_" + subPortfolioCode;

        // Aplicar el prefijo según el tipo de carga
        return loadType.getTablePrefix() + baseName;
    }

    /**
     * Verifica si una tabla dinámica existe
     */
    private boolean dynamicTableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Error al verificar existencia de tabla {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si una tabla tiene datos
     */
    private boolean hasDataInTable(String tableName) {
        try {
            String safeTableName = validateAndSanitizeTableName(tableName);
            String sql = "SELECT COUNT(*) FROM " + safeTableName;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Error al verificar datos en tabla {}: {}", tableName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verifica si una columna específica tiene datos no-NULL
     * Retorna true si hay al menos un valor no-NULL en la columna
     */
    private boolean columnHasNonNullData(String tableName, String columnName) {
        try {
            String safeTableName = validateAndSanitizeTableName(tableName);
            String safeColumnName = sanitizeColumnName(columnName);

            // Verificar primero si la columna existe en la tabla (usando parámetros preparados)
            String checkColumnSql = "SELECT COUNT(*) FROM information_schema.columns " +
                                   "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
            Integer columnExists = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, safeTableName, safeColumnName);

            if (columnExists == null || columnExists == 0) {
                logger.warn("La columna '{}' no existe en la tabla '{}'", safeColumnName, safeTableName);
                return false; // Si la columna no existe, no tiene datos
            }

            // Contar registros donde la columna tiene valor no-NULL
            String sql = "SELECT COUNT(*) FROM " + safeTableName + " WHERE " + safeColumnName + " IS NOT NULL";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            boolean hasData = count != null && count > 0;

            logger.debug("Columna '{}' en tabla '{}': {} registros con datos no-NULL",
                        safeColumnName, safeTableName, count);

            return hasData;
        } catch (Exception e) {
            logger.error("Error al verificar datos en columna '{}' de tabla '{}': {}",
                        columnName, tableName, e.getMessage(), e);
            // En caso de error, asumir que tiene datos para evitar pérdida accidental
            return true;
        }
    }

    /**
     * Verifica si una tabla tiene datos no-NULL en alguna de sus columnas (excluyendo el ID)
     * Retorna true si hay al menos un valor no-NULL en alguna columna de datos
     */
    private boolean tableHasAnyNonNullData(String tableName) {
        try {
            // Primero verificar si hay filas en la tabla
            if (!hasDataInTable(tableName)) {
                return false;
            }

            // Obtener todas las columnas de la tabla excepto 'id'
            String getColumnsSql = "SELECT column_name FROM information_schema.columns " +
                                  "WHERE table_schema = DATABASE() AND table_name = ? AND column_name != 'id'";
            List<String> columns = jdbcTemplate.queryForList(getColumnsSql, String.class, tableName);

            if (columns.isEmpty()) {
                return false;
            }

            // Verificar si alguna columna tiene datos no-NULL
            for (String column : columns) {
                if (columnHasNonNullData(tableName, column)) {
                    logger.debug("Columna '{}' tiene datos no-NULL en tabla '{}'", column, tableName);
                    return true;
                }
            }

            logger.debug("Tabla '{}' tiene {} filas pero todas las columnas tienen solo NULLs", tableName, columns.size());
            return false;

        } catch (Exception e) {
            logger.error("Error al verificar datos no-NULL en tabla '{}': {}", tableName, e.getMessage());
            // En caso de error, asumir que tiene datos para evitar pérdida accidental
            return true;
        }
    }

    /**
     * Agrega una columna a una tabla existente
     */
    private void addColumnToTable(String tableName, HeaderConfiguration header) {
        String columnName = sanitizeColumnName(header.getHeaderName());
        String dataType = header.getDataType();
        String format = header.getFormat();
        String sqlType = mapDataTypeToSQL(dataType, format);

        try {
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + sqlType;
            jdbcTemplate.execute(sql);
            logger.info("Columna {} agregada a tabla {}", columnName, tableName);
        } catch (Exception e) {
            logger.error("Error al agregar columna {} a tabla {}: {}", columnName, tableName, e.getMessage(), e);
            throw new RuntimeException("Error al agregar columna a la tabla: " + tableName, e);
        }
    }

    /**
     * Elimina una columna de una tabla existente
     */
    private void dropColumnFromTable(String tableName, HeaderConfiguration header) {
        String columnName = sanitizeColumnName(header.getHeaderName());

        try {
            String sql = "ALTER TABLE " + tableName + " DROP COLUMN " + columnName;
            jdbcTemplate.execute(sql);
            logger.info("Columna {} eliminada de tabla {}", columnName, tableName);
        } catch (Exception e) {
            logger.error("Error al eliminar columna {} de tabla {}: {}", columnName, tableName, e.getMessage(), e);
            throw new RuntimeException("Error al eliminar columna de la tabla: " + tableName, e);
        }
    }

    /**
     * Elimina una tabla dinámica completa
     */
    private void dropDynamicTable(String tableName) {
        try {
            String sql = "DROP TABLE IF EXISTS " + tableName;
            jdbcTemplate.execute(sql);
            logger.info("Tabla {} eliminada exitosamente", tableName);
        } catch (Exception e) {
            logger.error("Error al eliminar tabla {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Error al eliminar la tabla: " + tableName, e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> importDataToTable(Integer subPortfolioId, LoadType loadType, List<Map<String, Object>> data) {
        // Validar que la subcartera existe
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        String tableName = buildTableName(subPortfolio, loadType);

        // Verificar que la tabla existe
        if (!dynamicTableExists(tableName)) {
            throw new IllegalArgumentException("La tabla dinámica no existe para esta subcartera y tipo de carga. Debe configurar las cabeceras primero.");
        }

        // Obtener las configuraciones de cabeceras filtradas por tipo de carga
        List<HeaderConfiguration> headers = headerConfigurationRepository.findBySubPortfolioAndLoadType(subPortfolio, loadType);
        if (headers.isEmpty()) {
            throw new IllegalArgumentException("No hay cabeceras configuradas para esta subcartera y tipo de carga.");
        }

        // ========== BUSCAR CAMPO DE IDENTIFICACIÓN PARA UPSERT ==========
        // Buscar columnas de identificación: identity_code o nombre_cliente
        String identityColumnName = findIdentityColumn(headers, "identity_code", "codigo_identificacion", "cod_cli");
        String nameColumnName = findIdentityColumn(headers, "nombre_cliente", "nombre_completo", "full_name");

        logger.info("📋 Columnas para UPSERT - Identity: {}, Name: {}", identityColumnName, nameColumnName);

        // ========== CARGAR IDs EXISTENTES EN MEMORIA ==========
        Map<String, Integer> existingIdentityCodes = new HashMap<>();
        Map<String, Integer> existingNames = new HashMap<>();

        if (identityColumnName != null && columnExists(tableName, identityColumnName)) {
            existingIdentityCodes = loadExistingIdentifiers(tableName, identityColumnName);
            logger.info("📊 {} registros existentes cargados por identity_code", existingIdentityCodes.size());
        }

        if (nameColumnName != null && columnExists(tableName, nameColumnName)) {
            existingNames = loadExistingIdentifiers(tableName, nameColumnName);
            logger.info("📊 {} registros existentes cargados por nombre", existingNames.size());
        }

        // ========== PREPARAR Y CLASIFICAR FILAS ==========
        List<PreparedRowDataWithId> rowsToInsert = new ArrayList<>();
        List<PreparedRowDataWithId> rowsToUpdate = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int failedRows = 0;

        logger.info("📦 Preparando {} filas para UPSERT...", data.size());

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                PreparedRowData preparedRow = prepareRowData(row, headers, i + 1);

                // Extraer valores de identificación del row original
                String identityValue = extractIdentityValue(row, headers, identityColumnName);
                String nameValue = extractIdentityValue(row, headers, nameColumnName);

                // Buscar si existe el registro
                Integer existingId = null;

                // Primero buscar por identity_code (más preciso)
                if (identityValue != null && !identityValue.trim().isEmpty()) {
                    existingId = existingIdentityCodes.get(identityValue.toLowerCase().trim());
                }

                // Si no se encuentra, buscar por nombre
                if (existingId == null && nameValue != null && !nameValue.trim().isEmpty()) {
                    existingId = existingNames.get(nameValue.toLowerCase().trim());
                }

                PreparedRowDataWithId rowWithId = new PreparedRowDataWithId(
                    preparedRow.rowNumber, preparedRow.values, existingId, identityValue, nameValue
                );

                if (existingId != null) {
                    rowsToUpdate.add(rowWithId);
                } else {
                    rowsToInsert.add(rowWithId);
                }

            } catch (Exception e) {
                failedRows++;
                errors.add("Fila " + (i + 1) + ": " + e.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.debug("Error al validar fila {}: {}", i + 1, e.getMessage());
                }
            }
        }

        logger.info("✅ Clasificación: {} para INSERT, {} para UPDATE, {} con errores",
                   rowsToInsert.size(), rowsToUpdate.size(), failedRows);

        int insertedRows = 0;
        int updatedRows = 0;

        // ========== FASE 1: BATCH INSERT para registros nuevos ==========
        if (!rowsToInsert.isEmpty()) {
            try {
                List<PreparedRowData> simpleRows = rowsToInsert.stream()
                    .map(r -> new PreparedRowData(r.rowNumber, r.values))
                    .collect(java.util.stream.Collectors.toList());
                insertedRows = batchInsertRows(tableName, simpleRows, headers);
                logger.info("✅ Batch INSERT completado: {} filas insertadas", insertedRows);
            } catch (Exception e) {
                logger.error("❌ Error en batch INSERT: {}", e.getMessage(), e);
                throw new RuntimeException("Error al insertar datos: " + e.getMessage(), e);
            }
        }

        // ========== FASE 2: BATCH UPDATE para registros existentes ==========
        if (!rowsToUpdate.isEmpty()) {
            try {
                updatedRows = batchUpdateRows(tableName, rowsToUpdate, headers);
                logger.info("✅ Batch UPDATE completado: {} filas actualizadas", updatedRows);
            } catch (Exception e) {
                logger.error("❌ Error en batch UPDATE: {}", e.getMessage(), e);
                // No lanzar excepción para no perder los inserts exitosos
                errors.add("Error en actualización masiva: " + e.getMessage());
            }
        }

        // Preparar respuesta
        Map<String, Object> result = new HashMap<>();
        result.put("totalRows", data.size());
        result.put("insertedRows", insertedRows);
        result.put("updatedRows", updatedRows);
        result.put("failedRows", failedRows);
        result.put("tableName", tableName);

        if (!errors.isEmpty()) {
            result.put("errors", errors.size() > 20 ? errors.subList(0, 20) : errors);
            if (errors.size() > 20) {
                result.put("totalErrors", errors.size());
            }
        }

        logger.info("📊 Importación completada: {} insertadas, {} actualizadas, {} fallidas",
                   insertedRows, updatedRows, failedRows);

        // 🔄 Sincronizar clientes desde la tabla dinámica a la tabla clientes
        // SOLO sincronizar si el tipo de carga es INICIAL (la tabla maestra)
        // Para ACTUALIZACION, la sincronización se maneja en importDailyData() desde INICIAL
        if ((insertedRows > 0 || updatedRows > 0) && loadType == LoadType.INICIAL) {
            logger.info("🔄 Iniciando sincronización de clientes para SubPortfolio ID: {}, LoadType: {}", subPortfolioId, loadType);
            try {
                CustomerSyncService.SyncResult syncResult = customerSyncService.syncCustomersFromSubPortfolio(subPortfolioId.longValue(), loadType);
                logger.info("✅ Sincronización completada: {} clientes creados, {} actualizados",
                        syncResult.getCustomersCreated(), syncResult.getCustomersUpdated());

                result.put("syncCustomersCreated", syncResult.getCustomersCreated());
                result.put("syncCustomersUpdated", syncResult.getCustomersUpdated());
                if (syncResult.hasErrors()) {
                    result.put("syncErrors", syncResult.getErrors());
                }
            } catch (Exception e) {
                logger.error("❌ Error en sincronización de clientes: {}", e.getMessage(), e);
                result.put("syncError", "Error al sincronizar clientes: " + e.getMessage());
            }
        } else if (loadType == LoadType.ACTUALIZACION) {
            logger.info("⏭️ Omitiendo sincronización de clientes para ACTUALIZACION (se sincroniza desde INICIAL)");
        }

        return result;
    }

    /**
     * Busca una columna de identificación en las cabeceras configuradas
     */
    private String findIdentityColumn(List<HeaderConfiguration> headers, String... possibleNames) {
        for (String name : possibleNames) {
            String normalizedName = normalizeHeaderName(name);
            for (HeaderConfiguration header : headers) {
                if (normalizeHeaderName(header.getHeaderName()).equals(normalizedName)) {
                    return sanitizeColumnName(header.getHeaderName());
                }
                // También buscar en sourceField
                if (header.getSourceField() != null &&
                    normalizeHeaderName(header.getSourceField()).equals(normalizedName)) {
                    return sanitizeColumnName(header.getHeaderName());
                }
            }
        }
        return null;
    }

    /**
     * Carga identificadores existentes de la tabla en un mapa, usando paginación
     * para evitar problemas de memoria con tablas grandes
     */
    private Map<String, Integer> loadExistingIdentifiers(String tableName, String columnName) {
        Map<String, Integer> identifiers = new HashMap<>();

        // Validar nombres de tabla y columna
        String safeTableName = validateAndSanitizeTableName(tableName);
        String safeColumnName = sanitizeColumnName(columnName);

        try {
            // Primero contar el total de registros
            String countSql = "SELECT COUNT(*) FROM " + safeTableName +
                             " WHERE " + safeColumnName + " IS NOT NULL AND " + safeColumnName + " != ''";
            Integer totalCount = jdbcTemplate.queryForObject(countSql, Integer.class);

            if (totalCount == null || totalCount == 0) {
                logger.debug("No hay identificadores existentes en {}.{}", safeTableName, safeColumnName);
                return identifiers;
            }

            logger.info("Cargando {} identificadores de {}.{} en batches de {}",
                       totalCount, safeTableName, safeColumnName, BATCH_SIZE_FOR_IDENTIFIER_LOAD);

            // Cargar en batches para evitar problemas de memoria
            int offset = 0;
            while (offset < totalCount) {
                String sql = "SELECT id, " + safeColumnName + " FROM " + safeTableName +
                            " WHERE " + safeColumnName + " IS NOT NULL AND " + safeColumnName + " != ''" +
                            " LIMIT " + BATCH_SIZE_FOR_IDENTIFIER_LOAD + " OFFSET " + offset;

                jdbcTemplate.query(sql, rs -> {
                    String value = rs.getString(safeColumnName);
                    if (value != null && !value.trim().isEmpty()) {
                        identifiers.put(value.toLowerCase().trim(), rs.getInt("id"));
                    }
                });

                offset += BATCH_SIZE_FOR_IDENTIFIER_LOAD;

                if (offset < totalCount) {
                    logger.debug("Progreso: {}/{} identificadores cargados", Math.min(offset, totalCount), totalCount);
                }
            }

            logger.info("Total de {} identificadores cargados de {}.{}", identifiers.size(), safeTableName, safeColumnName);

        } catch (Exception e) {
            logger.error("Error cargando identificadores de {}.{}: {}", safeTableName, safeColumnName, e.getMessage(), e);
        }
        return identifiers;
    }

    /**
     * Extrae el valor de identificación de una fila
     */
    private String extractIdentityValue(Map<String, Object> row, List<HeaderConfiguration> headers, String columnName) {
        if (columnName == null) return null;

        // Buscar la cabecera correspondiente
        for (HeaderConfiguration header : headers) {
            if (sanitizeColumnName(header.getHeaderName()).equals(columnName)) {
                Object value;
                if (header.getSourceField() != null && !header.getSourceField().trim().isEmpty()) {
                    value = getValueFromRowData(row, header.getSourceField());
                } else {
                    value = getValueFromRowDataWithAliases(row, header);
                }
                return value != null ? value.toString() : null;
            }
        }
        return null;
    }

    /**
     * Clase para almacenar datos preparados con ID para UPSERT
     */
    private static class PreparedRowDataWithId extends PreparedRowData {
        private final Integer existingId;
        private final String identityValue;
        private final String nameValue;

        public PreparedRowDataWithId(int rowNumber, List<Object> values, Integer existingId,
                                     String identityValue, String nameValue) {
            super(rowNumber, values);
            this.existingId = existingId;
            this.identityValue = identityValue;
            this.nameValue = nameValue;
        }

        public Integer getExistingId() { return existingId; }
        public String getIdentityValue() { return identityValue; }
        public String getNameValue() { return nameValue; }
    }

    /**
     * Ejecuta batch UPDATE para filas existentes.
     * Procesa en batches de BATCH_SIZE_FOR_INSERT para mejor rendimiento.
     */
    private int batchUpdateRows(String tableName, List<PreparedRowDataWithId> rows, List<HeaderConfiguration> headers) {
        if (rows.isEmpty()) return 0;

        // Construir SQL de UPDATE
        StringBuilder setClause = new StringBuilder();
        boolean first = true;
        for (HeaderConfiguration header : headers) {
            String columnName = sanitizeColumnName(header.getHeaderName());
            if (!first) setClause.append(", ");
            setClause.append(columnName).append(" = ?");
            first = false;
        }

        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE id = ?";

        int totalUpdated = 0;
        int totalBatches = (int) Math.ceil((double) rows.size() / BATCH_SIZE_FOR_INSERT);

        logger.info("🔄 Ejecutando UPDATE de {} filas en {} batches en tabla {}", rows.size(), totalBatches, tableName);

        // Procesar en batches
        for (int batchStart = 0; batchStart < rows.size(); batchStart += BATCH_SIZE_FOR_INSERT) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE_FOR_INSERT, rows.size());
            List<PreparedRowDataWithId> currentBatch = rows.subList(batchStart, batchEnd);

            int[] updateCounts = jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    PreparedRowDataWithId rowData = currentBatch.get(i);
                    List<Object> values = rowData.getValues();

                    // Setear valores de las columnas
                    for (int j = 0; j < values.size(); j++) {
                        ps.setObject(j + 1, values.get(j));
                    }
                    // Setear el ID en el WHERE
                    ps.setInt(values.size() + 1, rowData.getExistingId());
                }

                @Override
                public int getBatchSize() {
                    return currentBatch.size();
                }
            });

            for (int count : updateCounts) {
                if (count > 0) totalUpdated += count;
            }
        }

        return totalUpdated;
    }

    /**
     * Clase interna para almacenar datos de fila preparada
     */
    private static class PreparedRowData {
        protected final int rowNumber;
        protected final List<Object> values;

        public PreparedRowData(int rowNumber, List<Object> values) {
            this.rowNumber = rowNumber;
            this.values = values;
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public List<Object> getValues() {
            return values;
        }
    }

    /**
     * Prepara una fila validando y convirtiendo los valores según las configuraciones de cabeceras
     * No inserta en BD, solo valida y prepara los datos
     */
    private PreparedRowData prepareRowData(Map<String, Object> rowData, List<HeaderConfiguration> headers, int rowNumber) {
        List<Object> values = new ArrayList<>();

        for (HeaderConfiguration header : headers) {
            // Obtener el valor: puede venir directo o por transformación
            Object value;
            if (header.getSourceField() != null && !header.getSourceField().trim().isEmpty()) {
                // Este campo se deriva de otro mediante transformación (case-insensitive)
                Object sourceValue = getValueFromRowData(rowData, header.getSourceField());
                if (sourceValue != null) {
                    String sourceStr = sourceValue.toString();

                    // Aplicar regex si está configurado
                    if (header.getRegexPattern() != null && !header.getRegexPattern().trim().isEmpty()) {
                        value = applyRegexTransformation(sourceStr, header.getRegexPattern(), header.getHeaderName());
                    } else {
                        // Sin regex, copiar el valor tal cual
                        value = sourceStr;
                    }
                } else {
                    value = null;
                }
            } else {
                // Campo normal: obtener directamente del CSV usando alias si es necesario
                value = getValueFromRowDataWithAliases(rowData, header);
            }

            // ========== VALIDACIÓN DE CAMPOS OBLIGATORIOS ==========
            boolean isEmpty = value == null ||
                             (value instanceof String && ((String) value).trim().isEmpty());

            // Validar que campos obligatorios tengan valor
            if (header.getRequired() != null && header.getRequired() == 1) {
                if (isEmpty) {
                    throw new IllegalArgumentException(
                        "El campo obligatorio '" + header.getHeaderName() + "' no puede estar vacío"
                    );
                }
            }

            // Si el campo NO es obligatorio y está vacío, convertir a NULL
            if (isEmpty) {
                value = null;
            }

            // Usar DataTypeValidator para validar y convertir el valor
            DataTypeValidator.ValidationResult validationResult = DataTypeValidator.validate(
                value,
                header.getDataType(),
                header.getFormat(),
                header.getHeaderName(),
                header.getRequired() != null && header.getRequired() == 1
            );

            if (!validationResult.isValid()) {
                throw new IllegalArgumentException(validationResult.getErrorMessage());
            }

            values.add(validationResult.getConvertedValue());
        }

        return new PreparedRowData(rowNumber, values);
    }

    /**
     * Inserta o actualiza filas usando batch operations (UPSERT)
     * Si un registro con el mismo identity_code ya existe, lo actualiza
     * Si no existe, lo inserta
     */
    private int batchInsertRows(String tableName, List<PreparedRowData> rows, List<HeaderConfiguration> headers) {
        if (rows.isEmpty()) {
            return 0;
        }

        // 1. Encontrar el índice de la columna identity_code
        int identityCodeIndex = -1;
        String identityCodeColumn = null;
        for (int i = 0; i < headers.size(); i++) {
            HeaderConfiguration header = headers.get(i);
            if (header.getFieldDefinition() != null &&
                "codigo_identificacion".equals(header.getFieldDefinition().getFieldCode())) {
                identityCodeIndex = i;
                identityCodeColumn = sanitizeColumnName(header.getHeaderName());
                logger.info("🔑 Columna identity_code encontrada: índice={}, columna={}", i, identityCodeColumn);
                break;
            }
        }

        // Si no encontramos identity_code, hacer INSERT normal (fallback al comportamiento anterior)
        if (identityCodeIndex == -1) {
            logger.warn("⚠️ No se encontró columna identity_code en headers, haciendo INSERT normal");
            return batchInsertNewRows(tableName, rows, headers);
        }

        // 2. Extraer los identity_codes del batch
        List<String> incomingCodes = new ArrayList<>();
        for (PreparedRowData row : rows) {
            Object value = row.getValues().get(identityCodeIndex);
            if (value != null) {
                incomingCodes.add(value.toString());
            }
        }

        // 3. Consultar cuáles ya existen en la tabla
        Set<String> existingCodes = queryExistingIdentityCodes(tableName, identityCodeColumn, incomingCodes);

        logger.info("📊 Análisis de datos: {} totales, {} ya existen en BD, {} son nuevos",
                   rows.size(), existingCodes.size(), rows.size() - existingCodes.size());

        // 4. Separar en dos grupos: existentes (UPDATE) y nuevos (INSERT)
        List<PreparedRowData> toInsert = new ArrayList<>();
        List<PreparedRowData> toUpdate = new ArrayList<>();

        for (PreparedRowData row : rows) {
            Object value = row.getValues().get(identityCodeIndex);
            String code = value != null ? value.toString() : null;

            if (code != null && existingCodes.contains(code)) {
                toUpdate.add(row);
            } else {
                toInsert.add(row);
            }
        }

        int totalProcessed = 0;

        // 5. UPDATE para registros existentes
        if (!toUpdate.isEmpty()) {
            logger.info("🔄 Actualizando {} registros existentes...", toUpdate.size());
            int updated = batchUpdateRows(tableName, toUpdate, headers, identityCodeIndex, identityCodeColumn);
            logger.info("✅ {} registros actualizados", updated);
            totalProcessed += updated;
        }

        // 6. INSERT para registros nuevos
        if (!toInsert.isEmpty()) {
            logger.info("➕ Insertando {} registros nuevos...", toInsert.size());
            int inserted = batchInsertNewRows(tableName, toInsert, headers);
            logger.info("✅ {} registros insertados", inserted);
            totalProcessed += inserted;
        }

        return totalProcessed;
    }

    /**
     * Consulta qué identity_codes ya existen en la tabla.
     * Procesa en batches para evitar IN clauses muy grandes (límite ~1000 elementos).
     */
    private Set<String> queryExistingIdentityCodes(String tableName, String identityCodeColumn, List<String> codes) {
        if (codes.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> existingCodes = new HashSet<>();
        int batchSize = 500; // Tamaño seguro para IN clause

        // Procesar en batches para evitar IN clause muy grande
        for (int i = 0; i < codes.size(); i += batchSize) {
            int end = Math.min(i + batchSize, codes.size());
            List<String> batch = codes.subList(i, end);

            String placeholders = batch.stream().map(c -> "?").collect(java.util.stream.Collectors.joining(", "));
            String sql = "SELECT " + identityCodeColumn + " FROM " + tableName +
                         " WHERE " + identityCodeColumn + " IN (" + placeholders + ")";

            try {
                List<String> batchResult = jdbcTemplate.queryForList(sql, String.class, batch.toArray());
                existingCodes.addAll(batchResult);
            } catch (Exception e) {
                logger.error("❌ Error al consultar identity_codes existentes (batch {}-{}): {}",
                           i, end, e.getMessage());
            }
        }

        return existingCodes;
    }

    /**
     * Batch UPDATE para registros existentes.
     * Procesa en batches de BATCH_SIZE_FOR_INSERT para mejor rendimiento.
     */
    private int batchUpdateRows(String tableName, List<PreparedRowData> rows,
                                List<HeaderConfiguration> headers, int identityCodeIndex,
                                String identityCodeColumn) {
        if (rows.isEmpty()) {
            return 0;
        }

        // Construir SQL: UPDATE tabla SET col1=?, col2=? WHERE identity_code=?
        StringBuilder setClause = new StringBuilder();
        boolean first = true;

        for (int i = 0; i < headers.size(); i++) {
            if (i == identityCodeIndex) continue; // No actualizar la columna identity_code

            String columnName = sanitizeColumnName(headers.get(i).getHeaderName());
            if (first) {
                setClause.append(columnName).append(" = ?");
                first = false;
            } else {
                setClause.append(", ").append(columnName).append(" = ?");
            }
        }

        String sql = "UPDATE " + tableName + " SET " + setClause +
                     " WHERE " + identityCodeColumn + " = ?";

        final int idxCodeIndex = identityCodeIndex;
        int totalUpdated = 0;
        int totalBatches = (int) Math.ceil((double) rows.size() / BATCH_SIZE_FOR_INSERT);

        logger.info("🔄 Ejecutando UPDATE de {} filas en {} batches", rows.size(), totalBatches);

        // Procesar en batches
        for (int batchStart = 0; batchStart < rows.size(); batchStart += BATCH_SIZE_FOR_INSERT) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE_FOR_INSERT, rows.size());
            List<PreparedRowData> currentBatch = rows.subList(batchStart, batchEnd);

            int[] updateCounts = jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    PreparedRowData rowData = currentBatch.get(i);
                    List<Object> values = rowData.getValues();

                    int paramIndex = 1;
                    // Setear valores de columnas (excepto identity_code)
                    for (int j = 0; j < values.size(); j++) {
                        if (j == idxCodeIndex) continue;
                        ps.setObject(paramIndex++, values.get(j));
                    }
                    // Setear identity_code para el WHERE
                    ps.setObject(paramIndex, values.get(idxCodeIndex));
                }

                @Override
                public int getBatchSize() {
                    return currentBatch.size();
                }
            });

            for (int count : updateCounts) {
                if (count > 0) {
                    totalUpdated += count;
                }
            }
        }

        return totalUpdated;
    }

    /**
     * Batch INSERT para registros nuevos
     */
    private int batchInsertNewRows(String tableName, List<PreparedRowData> rows, List<HeaderConfiguration> headers) {
        if (rows.isEmpty()) {
            return 0;
        }

        // Construir SQL una sola vez
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        boolean firstColumn = true;

        for (HeaderConfiguration header : headers) {
            String columnName = sanitizeColumnName(header.getHeaderName());

            if (firstColumn) {
                columns.append(columnName);
                placeholders.append("?");
                firstColumn = false;
            } else {
                columns.append(", ").append(columnName);
                placeholders.append(", ?");
            }
        }

        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";

        int totalInserted = 0;
        int totalBatches = (int) Math.ceil((double) rows.size() / BATCH_SIZE_FOR_INSERT);

        logger.info("🚀 Ejecutando INSERT de {} filas en {} batches de máximo {} registros",
                   rows.size(), totalBatches, BATCH_SIZE_FOR_INSERT);

        // Procesar en batches para evitar problemas de memoria y mejorar rendimiento
        for (int batchStart = 0; batchStart < rows.size(); batchStart += BATCH_SIZE_FOR_INSERT) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE_FOR_INSERT, rows.size());
            List<PreparedRowData> currentBatch = rows.subList(batchStart, batchEnd);

            int[] updateCounts = jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    PreparedRowData rowData = currentBatch.get(i);
                    List<Object> values = rowData.getValues();

                    for (int j = 0; j < values.size(); j++) {
                        ps.setObject(j + 1, values.get(j));
                    }
                }

                @Override
                public int getBatchSize() {
                    return currentBatch.size();
                }
            });

            // Sumar registros insertados en este batch
            for (int count : updateCounts) {
                if (count > 0) {
                    totalInserted += count;
                }
            }

            // Log de progreso cada 5 batches o al final
            int currentBatchNum = (batchStart / BATCH_SIZE_FOR_INSERT) + 1;
            if (currentBatchNum % 5 == 0 || batchEnd == rows.size()) {
                logger.info("📊 Progreso INSERT: {}/{} batches ({} registros)", currentBatchNum, totalBatches, batchEnd);
            }
        }

        return totalInserted;
    }

    /**
     * Inserta una fila en la tabla dinámica y crea el cliente si es necesario
     * @deprecated Usar batchInsertRows para mejor rendimiento
     */
    @Deprecated
    private void insertRowToTable(String tableName, Map<String, Object> rowData, List<HeaderConfiguration> headers, SubPortfolio subPortfolio) {
        // Construir SQL dinámico
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();

        boolean firstColumn = true;

        logger.debug("🔍 Insertando fila. Cabeceras configuradas: {}, Keys en CSV: {}",
            headers.stream().map(HeaderConfiguration::getHeaderName).collect(java.util.stream.Collectors.toList()),
            rowData.keySet());

        // Agregar columnas dinámicas
        for (HeaderConfiguration header : headers) {
            String columnName = sanitizeColumnName(header.getHeaderName());

            // Obtener el valor: puede venir directo o por transformación
            Object value;
            if (header.getSourceField() != null && !header.getSourceField().trim().isEmpty()) {
                // Este campo se deriva de otro mediante transformación (case-insensitive)
                Object sourceValue = getValueFromRowData(rowData, header.getSourceField());
                if (sourceValue != null) {
                    String sourceStr = sourceValue.toString();

                    // Aplicar regex si está configurado
                    if (header.getRegexPattern() != null && !header.getRegexPattern().trim().isEmpty()) {
                        value = applyRegexTransformation(sourceStr, header.getRegexPattern(), header.getHeaderName());
                    } else {
                        // Sin regex, copiar el valor tal cual
                        value = sourceStr;
                    }
                    logger.debug("Transformación: {} → {}", header.getSourceField(), header.getHeaderName());
                } else {
                    value = null;
                }
            } else {
                // Campo normal: obtener directamente del CSV (case-insensitive)
                value = getValueFromRowData(rowData, header.getHeaderName());
            }

            // ========== VALIDACIÓN DE CAMPOS OBLIGATORIOS ==========
            boolean isEmpty = value == null ||
                             (value instanceof String && ((String) value).trim().isEmpty());

            // Validar que campos obligatorios tengan valor
            if (header.getRequired() != null && header.getRequired() == 1) {
                if (isEmpty) {
                    throw new IllegalArgumentException(
                        "El campo obligatorio '" + header.getHeaderName() + "' no puede estar vacío"
                    );
                }
            }

            // Si el campo NO es obligatorio y está vacío, convertir a NULL
            if (isEmpty) {
                value = null;
            }

            if (firstColumn) {
                columns.append(columnName);
                placeholders.append("?");
                firstColumn = false;
            } else {
                columns.append(", ").append(columnName);
                placeholders.append(", ?");
            }

            // Convertir el valor según el tipo de dato
            if (value == null) {
                values.add(null);
            } else {
                switch (header.getDataType().toUpperCase()) {
                    case "NUMERICO":
                        // Intentar convertir a número
                        try {
                            if (value instanceof Number) {
                                values.add(value);
                            } else {
                                String numStr = value.toString().trim();

                                // Manejar casos como ".10" o ".20" agregando "0" al inicio
                                if (numStr.startsWith(".")) {
                                    numStr = "0" + numStr;
                                }

                                values.add(Double.parseDouble(numStr));
                            }
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Valor no numérico para campo " + header.getHeaderName() + ": " + value);
                        }
                        break;
                    case "FECHA":
                        // Convertir a fecha usando parseo flexible
                        // IMPORTANTE: Siempre se guarda como LocalDate en BD (sin hora)
                        try {
                            if (value instanceof LocalDate) {
                                values.add(value);
                            } else if (value instanceof java.time.LocalDateTime) {
                                values.add(((java.time.LocalDateTime) value).toLocalDate());
                            } else {
                                String dateStr = value.toString().trim();
                                LocalDate parsedDate = parseFlexibleDate(dateStr, header.getFormat());
                                values.add(parsedDate);
                            }
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Valor no es fecha válida para campo " + header.getHeaderName() + ": " + value +
                                " (formato esperado: " + (header.getFormat() != null ? header.getFormat() : "auto-detectado") + ")" +
                                " - Error: " + e.getMessage());
                        }
                        break;
                    case "BOOLEANO":
                    case "BOOLEAN":
                        // Convertir a booleano (1 o 0 para BIT en SQL Server)
                        try {
                            if (value instanceof Boolean) {
                                values.add((Boolean) value ? 1 : 0);
                            } else {
                                String boolStr = value.toString().trim().toLowerCase();
                                boolean boolValue = boolStr.equals("true") || boolStr.equals("1") ||
                                    boolStr.equals("si") || boolStr.equals("sí") ||
                                    boolStr.equals("yes") || boolStr.equals("verdadero") ||
                                    boolStr.equals("v") || boolStr.equals("y") ||
                                    boolStr.equals("activo") || boolStr.equals("habilitado");
                                values.add(boolValue ? 1 : 0);
                            }
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Valor no es booleano válido para campo " + header.getHeaderName() + ": " + value);
                        }
                        break;
                    case "TEXTO":
                    default:
                        values.add(value.toString());
                        break;
                }
            }
        }

        // Ejecutar INSERT
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";

        try {
            jdbcTemplate.update(sql, values.toArray());

            // Después de insertar exitosamente, crear el cliente si es necesario
            createCustomerIfNeeded(rowData, headers, subPortfolio);
        } catch (Exception e) {
            logger.error("Error al ejecutar INSERT en {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Error al insertar datos: " + e.getMessage(), e);
        }
    }

    /**
     * Crea un cliente en la tabla clientes si no existe, usando los datos transformados
     */
    private void createCustomerIfNeeded(Map<String, Object> rowData, List<HeaderConfiguration> headers, SubPortfolio subPortfolio) {
        try {
            // Construir un mapa de valores transformados: fieldCode -> valor
            Map<String, String> transformedData = new HashMap<>();

            for (HeaderConfiguration header : headers) {
                // Obtener el valor (puede ser transformado o directo)
                String value;
                if (header.getSourceField() != null && !header.getSourceField().trim().isEmpty()) {
                    // Campo transformado
                    Object sourceValue = rowData.get(header.getSourceField());
                    if (sourceValue != null) {
                        String sourceStr = sourceValue.toString();
                        if (header.getRegexPattern() != null && !header.getRegexPattern().trim().isEmpty()) {
                            value = applyRegexTransformation(sourceStr, header.getRegexPattern(), header.getHeaderName());
                        } else {
                            value = sourceStr;
                        }
                    } else {
                        value = null;
                    }
                } else {
                    // Campo directo
                    Object directValue = rowData.get(header.getHeaderName());
                    value = directValue != null ? directValue.toString() : null;
                }

                // Guardar en el mapa usando el fieldCode de la definición
                if (header.getFieldDefinition() != null && value != null) {
                    String fieldCode = header.getFieldDefinition().getFieldCode();
                    transformedData.put(fieldCode, value);

                } else if (header.getFieldDefinition() == null) {
                    logger.warn("⚠️ Header '{}' NO tiene FieldDefinition asociado", header.getHeaderName());
                }
            }

            // Extraer campos necesarios para crear el cliente
            String documento = transformedData.get("documento");
            String identificationCode = transformedData.get("codigo_identificacion");
            String nombreCompleto = transformedData.get("nombre_completo");

            // Si no tenemos codigo_identificacion, no podemos crear el cliente
            if (identificationCode == null || identificationCode.trim().isEmpty()) {
                logger.debug("No se puede crear cliente: falta campo 'codigo_identificacion'");
                return;
            }

            // Obtener tenantId
            Long tenantId = subPortfolio.getPortfolio().getTenant().getId().longValue();

            // Verificar si el cliente ya existe
            Optional<Customer> existingCustomer = customerRepository.findByTenantIdAndIdentificationCode(tenantId, identificationCode);

            if (existingCustomer.isEmpty()) {
                // Crear nuevo cliente con campos básicos
                Customer newCustomer = new Customer(
                    tenantId,
                    identificationCode,  // identificationCode (OBLIGATORIO)
                    documento,           // document (opcional)
                    nombreCompleto,      // fullName (opcional)
                    null,               // birthDate (se setea después si existe)
                    "ACTIVO"            // status
                );

                // Setear campos adicionales dinámicamente desde transformedData
                String primerNombre = transformedData.get("primer_nombre");
                if (primerNombre != null && !primerNombre.trim().isEmpty()) {
                    newCustomer.setFirstName(primerNombre);
                }

                String segundoNombre = transformedData.get("segundo_nombre");
                if (segundoNombre != null && !segundoNombre.trim().isEmpty()) {
                    newCustomer.setSecondName(segundoNombre);
                }

                String primerApellido = transformedData.get("primer_apellido");
                if (primerApellido != null && !primerApellido.trim().isEmpty()) {
                    newCustomer.setFirstLastName(primerApellido);
                }

                String segundoApellido = transformedData.get("segundo_apellido");
                if (segundoApellido != null && !segundoApellido.trim().isEmpty()) {
                    newCustomer.setSecondLastName(segundoApellido);
                }

                String fechaNacimientoStr = transformedData.get("fecha_nacimiento");
                if (fechaNacimientoStr != null && !fechaNacimientoStr.trim().isEmpty()) {
                    try {
                        LocalDate fechaNacimiento = LocalDate.parse(fechaNacimientoStr);
                        newCustomer.setBirthDate(fechaNacimiento);
                    } catch (Exception e) {
                        logger.warn("Error parseando fecha_nacimiento: {}", fechaNacimientoStr);
                    }
                }

                String edadStr = transformedData.get("edad");
                if (edadStr != null && !edadStr.trim().isEmpty()) {
                    try {
                        newCustomer.setAge(Integer.parseInt(edadStr));
                    } catch (NumberFormatException e) {
                        logger.warn("Error parseando edad: {}", edadStr);
                    }
                }

                String estadoCivil = transformedData.get("estado_civil");
                if (estadoCivil != null && !estadoCivil.trim().isEmpty()) {
                    newCustomer.setMaritalStatus(estadoCivil);
                }

                String ocupacion = transformedData.get("ocupacion");
                if (ocupacion != null && !ocupacion.trim().isEmpty()) {
                    newCustomer.setOccupation(ocupacion);
                }

                String tipoCliente = transformedData.get("tipo_cliente");
                if (tipoCliente != null && !tipoCliente.trim().isEmpty()) {
                    newCustomer.setCustomerType(tipoCliente);
                }

                String direccion = transformedData.get("direccion");
                if (direccion != null && !direccion.trim().isEmpty()) {
                    newCustomer.setAddress(direccion);
                }

                String distrito = transformedData.get("distrito");
                if (distrito != null && !distrito.trim().isEmpty()) {
                    newCustomer.setDistrict(distrito);
                }

                String provincia = transformedData.get("provincia");
                if (provincia != null && !provincia.trim().isEmpty()) {
                    newCustomer.setProvince(provincia);
                }

                String departamento = transformedData.get("departamento");
                if (departamento != null && !departamento.trim().isEmpty()) {
                    newCustomer.setDepartment(departamento);
                }

                String referenciaPersonal = transformedData.get("referencia_personal");
                if (referenciaPersonal != null && !referenciaPersonal.trim().isEmpty()) {
                    newCustomer.setPersonalReference(referenciaPersonal);
                }

                String numeroCuentaLineaPrestamo = transformedData.get("numero_cuenta_linea_prestamo");
                if (numeroCuentaLineaPrestamo != null && !numeroCuentaLineaPrestamo.trim().isEmpty()) {
                    newCustomer.setAccountNumber(numeroCuentaLineaPrestamo);
                }

                Customer savedCustomer = customerRepository.save(newCustomer);

                // Crear métodos de contacto para el nuevo cliente
                createContactMethodsForCustomer(savedCustomer, transformedData, headers);
            } else {
                logger.debug("Cliente ya existe con codigo_identificacion={}", identificationCode);

                // Actualizar accountNumber si viene en los datos
                Customer customer = existingCustomer.get();
                String numeroCuentaLineaPrestamo = transformedData.get("numero_cuenta_linea_prestamo");
                if (numeroCuentaLineaPrestamo != null && !numeroCuentaLineaPrestamo.trim().isEmpty()) {
                    customer.setAccountNumber(numeroCuentaLineaPrestamo);
                    customerRepository.save(customer);
                    logger.debug("AccountNumber actualizado para cliente existente: {}", identificationCode);
                }

                // También crear métodos de contacto para clientes existentes
                createContactMethodsForCustomer(customer, transformedData, headers);
            }

        } catch (Exception e) {
            // No lanzar excepción para no interrumpir la importación
            // Solo registrar el error
            logger.error("Error al crear cliente (no crítico): {}", e.getMessage());
        }
    }

    /**
     * Obtiene un valor del rowData de forma case-insensitive y flexible con espacios/guiones
     */
    private Object getValueFromRowData(Map<String, Object> rowData, String headerName) {
        if (headerName == null) return null;

        // Primero intentar búsqueda exacta
        if (rowData.containsKey(headerName)) {
            return rowData.get(headerName);
        }

        // Normalizar el nombre buscado para comparación flexible
        String normalizedSearch = normalizeHeaderName(headerName);

        // Buscar en todas las keys del mapa
        for (Map.Entry<String, Object> entry : rowData.entrySet()) {
            String key = entry.getKey();
            if (key != null && normalizeHeaderName(key).equals(normalizedSearch)) {
                logger.debug("Mapeo flexible: '{}' encontrado como '{}' en CSV", headerName, key);
                return entry.getValue();
            }
        }

        // No encontrado
        logger.warn("Cabecera '{}' no encontrada en el CSV. Keys disponibles: {}",
                   headerName, rowData.keySet());
        return null;
    }

    /**
     * Obtiene un valor del rowData usando el nombre principal y todos los alias de la cabecera
     * Esto permite que el archivo Excel use cualquiera de los nombres configurados como alias
     */
    private Object getValueFromRowDataWithAliases(Map<String, Object> rowData, HeaderConfiguration header) {
        if (header == null) return null;

        // 1. Primero buscar por nombre principal
        Object value = getValueFromRowData(rowData, header.getHeaderName());
        if (value != null) {
            return value;
        }

        // 2. Si no encontró, buscar por cada alias
        if (header.getAliases() != null && !header.getAliases().isEmpty()) {
            for (var alias : header.getAliases()) {
                if (alias.getAlias() != null && !alias.getAlias().equalsIgnoreCase(header.getHeaderName())) {
                    value = getValueFromRowData(rowData, alias.getAlias());
                    if (value != null) {
                        logger.debug("Valor encontrado por alias: '{}' -> '{}' para cabecera '{}'",
                                    alias.getAlias(), value, header.getHeaderName());
                        return value;
                    }
                }
            }
        }

        // 3. No encontrado por ningún nombre
        return null;
    }

    /**
     * Parsea una fecha de forma flexible, intentando múltiples formatos comunes.
     * Delegado a DateParserUtil para centralizar la lógica de parseo de fechas.
     *
     * @param dateStr String con la fecha a parsear
     * @param configuredFormat Formato configurado en la cabecera (puede ser null)
     * @return LocalDate parseado
     * @throws IllegalArgumentException si no se puede parsear con ningún formato
     */
    private LocalDate parseFlexibleDate(String dateStr, String configuredFormat) {
        return DateParserUtil.parseFlexibleDate(dateStr, configuredFormat);
    }

    /**
     * Normaliza un nombre de cabecera para comparación flexible:
     * - A minúsculas
     * - Espacios y guiones bajos se tratan igual
     * - Sin acentos
     * - Sin caracteres especiales excepto letras, números y _
     */
    private String normalizeHeaderName(String headerName) {
        if (headerName == null) return "";

        return headerName
                .toLowerCase()
                .trim()
                // Normalizar acentos
                .replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("[ñ]", "n")
                // Reemplazar espacios por guión bajo para normalizar
                .replaceAll("\\s+", "_")
                // Eliminar caracteres especiales excepto letras, números y guión bajo
                .replaceAll("[^a-z0-9_]", "")
                // Eliminar guiones bajos duplicados
                .replaceAll("_+", "_")
                // Eliminar guiones bajos al inicio o final
                .replaceAll("^_|_$", "");
    }
    
    /**
     * Aplica una transformación regex a un valor
     */
    private String applyRegexTransformation(String sourceValue, String regexPattern, String fieldName) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regexPattern);
            java.util.regex.Matcher matcher = pattern.matcher(sourceValue);

            if (matcher.find()) {
                // Si el regex tiene grupos de captura, extraer el primero
                if (matcher.groupCount() > 0) {
                    return matcher.group(1);
                } else {
                    // Si no tiene grupos, retornar el match completo
                    return matcher.group(0);
                }
            } else {
                logger.warn("⚠️ Regex no coincidió para campo {}: patrón='{}' valor='{}'",
                           fieldName, regexPattern, sourceValue);
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ Error aplicando regex para campo {}: {}", fieldName, e.getMessage());
            throw new IllegalArgumentException("Error en transformación regex para campo " + fieldName + ": " + e.getMessage());
        }
    }

    /**
     * Crea métodos de contacto (teléfonos y emails) para un cliente
     */
    private void createContactMethodsForCustomer(Customer customer, Map<String, String> transformedData, List<HeaderConfiguration> headers) {
        try {
            // Definir los campos de contacto y sus tipos
            Map<String, String> contactFields = Map.of(
                "telefono_principal", "telefono",
                "telefono_secundario", "telefono",
                "telefono_trabajo", "telefono",
                "telefono_referencia_1", "telefono",
                "telefono_referencia_2", "telefono",
                "email", "email"
            );
            

            LocalDate today = LocalDate.now();
            int contactMethodsCreated = 0;

            // Iterar sobre cada campo de contacto
            for (Map.Entry<String, String> entry : contactFields.entrySet()) {
                String fieldCode = entry.getKey();
                String contactType = entry.getValue();

                // Obtener el valor del campo desde transformedData
                String value = transformedData.get(fieldCode);

                if (value != null && !value.trim().isEmpty()) {
                    // Buscar la etiqueta (headerName) correspondiente
                    String label = headers.stream()
                            .filter(h -> h.getFieldDefinition() != null &&
                                       fieldCode.equals(h.getFieldDefinition().getFieldCode()))
                            .map(HeaderConfiguration::getHeaderName)
                            .findFirst()
                            .orElse(fieldCode); // Si no se encuentra, usar el fieldCode

                    // Crear el método de contacto
                    ContactMethod contactMethod = ContactMethod.builder()
                            .customer(customer)
                            .contactType(contactType)
                            .subtype(fieldCode)
                            .value(value)
                            .label(label)
                            .importDate(today)
                            .status("ACTIVO")
                            .build();

                    contactMethodRepository.save(contactMethod);
                    contactMethodsCreated++;

                    logger.debug("✅ Método de contacto creado: tipo={}, subtipo={}, valor={}",
                               contactType, fieldCode, value);
                }
            }

            if (contactMethodsCreated > 0) {
                logger.info("✅ Se crearon {} métodos de contacto para el cliente ID={}",
                           contactMethodsCreated, customer.getId());
            }

        } catch (Exception e) {
            logger.error("Error al crear métodos de contacto para cliente ID={}: {}",
                        customer.getId(), e.getMessage());
        }
    }

    // ==================== ACTUALIZACIÓN DE DATOS COMPLEMENTARIOS ====================

    // Tamaño del batch para operaciones de UPDATE complementario
    private static final int BATCH_SIZE_FOR_COMPLEMENTARY_UPDATE = 500;

    @Override
    @Transactional
    public Map<String, Object> updateComplementaryDataInTable(Integer subPortfolioId, LoadType loadType,
                                                               List<Map<String, Object>> data, String linkField) {
        long startTime = System.currentTimeMillis();
        logger.info("📦 Actualizando datos complementarios: subPortfolioId={}, loadType={}, linkField={}, rows={}",
                   subPortfolioId, loadType, linkField, data.size());

        // Validar que la subcartera existe
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        String tableName = buildTableName(subPortfolio, loadType);

        // Verificar que la tabla existe
        if (!dynamicTableExists(tableName)) {
            throw new IllegalArgumentException("La tabla dinámica no existe. Debe cargar primero el archivo principal con los datos iniciales.");
        }

        // Obtener las configuraciones de cabeceras
        List<HeaderConfiguration> headers = headerConfigurationRepository.findBySubPortfolioAndLoadType(subPortfolio, loadType);
        if (headers.isEmpty()) {
            throw new IllegalArgumentException("No hay cabeceras configuradas para esta subcartera y tipo de carga.");
        }

        // ========== OPTIMIZACIÓN 1: Cargar todas las columnas existentes en la tabla UNA SOLA VEZ ==========
        Set<String> existingColumns = loadTableColumns(tableName);
        logger.info("📊 Columnas existentes en tabla: {}", existingColumns.size());

        // ========== OPTIMIZACIÓN 2: Pre-computar mapeo de cabeceras ==========
        Map<String, HeaderConfiguration> headerMap = new HashMap<>();
        for (HeaderConfiguration header : headers) {
            String normalizedName = normalizeHeaderName(header.getHeaderName());
            headerMap.put(normalizedName, header);
        }

        // Buscar la columna de enlace
        String linkColumnName = findLinkColumnName(linkField, headers);
        logger.info("✅ Campo de enlace '{}' mapeado a columna '{}'", linkField, linkColumnName);

        // ========== OPTIMIZACIÓN 3: Identificar columnas a actualizar (solo una vez) ==========
        if (data.isEmpty()) {
            return buildComplementaryResult(0, 0, 0, 0, tableName, new ArrayList<>(), startTime);
        }

        // Obtener las columnas del primer registro para determinar estructura
        Map<String, Object> sampleRow = data.get(0);
        List<ColumnUpdateInfo> columnsToUpdate = new ArrayList<>();

        for (String columnKey : sampleRow.keySet()) {
            // Saltar el campo de enlace
            if (normalizeHeaderName(columnKey).equals(normalizeHeaderName(linkField))) {
                continue;
            }

            String sanitizedColumn = sanitizeColumnName(columnKey);

            // Verificar si la columna existe (usando el cache)
            if (existingColumns.contains(sanitizedColumn.toLowerCase())) {
                HeaderConfiguration matchingHeader = headerMap.get(normalizeHeaderName(columnKey));
                columnsToUpdate.add(new ColumnUpdateInfo(columnKey, sanitizedColumn, matchingHeader));
            } else {
                logger.debug("Columna '{}' no existe en tabla, ignorando", sanitizedColumn);
            }
        }

        if (columnsToUpdate.isEmpty()) {
            logger.warn("No hay columnas válidas para actualizar");
            return buildComplementaryResult(data.size(), 0, 0, data.size(), tableName,
                List.of("No se encontraron columnas válidas para actualizar"), startTime);
        }

        logger.info("📝 Columnas a actualizar: {}", columnsToUpdate.stream()
            .map(c -> c.sanitizedName).toList());

        // ========== OPTIMIZACIÓN 4: Construir SQL una sola vez ==========
        String updateSql = buildComplementaryUpdateSql(tableName, columnsToUpdate, linkColumnName);
        logger.debug("SQL de actualización: {}", updateSql);

        // ========== OPTIMIZACIÓN 5: Procesar en batches ==========
        int updatedRows = 0;
        int notFoundRows = 0;
        int failedRows = 0;
        List<String> errors = new ArrayList<>();

        int totalBatches = (int) Math.ceil((double) data.size() / BATCH_SIZE_FOR_COMPLEMENTARY_UPDATE);
        logger.info("📦 Procesando {} registros en {} batches de hasta {} registros",
                   data.size(), totalBatches, BATCH_SIZE_FOR_COMPLEMENTARY_UPDATE);

        for (int batchNum = 0; batchNum < totalBatches; batchNum++) {
            int startIdx = batchNum * BATCH_SIZE_FOR_COMPLEMENTARY_UPDATE;
            int endIdx = Math.min(startIdx + BATCH_SIZE_FOR_COMPLEMENTARY_UPDATE, data.size());
            List<Map<String, Object>> batch = data.subList(startIdx, endIdx);

            try {
                int[] results = executeBatchComplementaryUpdate(batch, updateSql, columnsToUpdate, linkField, errors, startIdx);

                for (int i = 0; i < results.length; i++) {
                    if (results[i] > 0) {
                        updatedRows += results[i];
                    } else if (results[i] == 0) {
                        notFoundRows++;
                    }
                }

                if ((batchNum + 1) % 10 == 0 || batchNum == totalBatches - 1) {
                    logger.info("📊 Progreso: {}/{} batches ({} registros procesados)",
                               batchNum + 1, totalBatches, endIdx);
                }

            } catch (Exception e) {
                logger.error("Error en batch {}: {}", batchNum + 1, e.getMessage());
                // Marcar todo el batch como fallido
                failedRows += batch.size();
                errors.add("Batch " + (batchNum + 1) + " falló: " + e.getMessage());
            }
        }

        // Contar filas fallidas basado en errores individuales
        failedRows = errors.size();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("✅ Actualización complementaria completada en {}ms: {} actualizados, {} no encontrados, {} fallidos",
                   duration, updatedRows, notFoundRows, failedRows);

        return buildComplementaryResult(data.size(), updatedRows, notFoundRows, failedRows, tableName, errors, startTime);
    }

    /**
     * Carga todas las columnas existentes en una tabla (optimización para evitar consultas repetidas)
     */
    private Set<String> loadTableColumns(String tableName) {
        String safeTableName = validateAndSanitizeTableName(tableName);
        String sql = "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = ?";
        List<String> columns = jdbcTemplate.queryForList(sql, String.class, safeTableName);
        return columns.stream()
            .map(String::toLowerCase)
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Encuentra el nombre de columna de enlace en las cabeceras
     */
    private String findLinkColumnName(String linkField, List<HeaderConfiguration> headers) {
        String normalizedLinkField = normalizeHeaderName(linkField);

        for (HeaderConfiguration header : headers) {
            if (normalizeHeaderName(header.getHeaderName()).equals(normalizedLinkField)) {
                return sanitizeColumnName(header.getHeaderName());
            }
            if (header.getSourceField() != null && normalizeHeaderName(header.getSourceField()).equals(normalizedLinkField)) {
                return sanitizeColumnName(header.getHeaderName());
            }
        }

        // Si no se encontró, usar nombre sanitizado directamente
        logger.warn("⚠️ Campo de enlace '{}' no encontrado en cabeceras, usando nombre directo", linkField);
        return sanitizeColumnName(linkField);
    }

    /**
     * Información de una columna a actualizar
     */
    private record ColumnUpdateInfo(String originalName, String sanitizedName, HeaderConfiguration header) {}

    /**
     * Construye el SQL de UPDATE para carga complementaria
     */
    private String buildComplementaryUpdateSql(String tableName, List<ColumnUpdateInfo> columns, String linkColumnName) {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(tableName);
        sql.append(" SET ");

        boolean first = true;
        for (ColumnUpdateInfo col : columns) {
            if (!first) {
                sql.append(", ");
            }
            sql.append(col.sanitizedName).append(" = ?");
            first = false;
        }

        sql.append(" WHERE ").append(linkColumnName).append(" = ?");
        return sql.toString();
    }

    /**
     * Ejecuta un batch de actualizaciones complementarias
     */
    private int[] executeBatchComplementaryUpdate(List<Map<String, Object>> batch, String sql,
                                                   List<ColumnUpdateInfo> columnsToUpdate, String linkField,
                                                   List<String> errors, int startIdx) {
        List<Object[]> batchArgs = new ArrayList<>();

        for (int i = 0; i < batch.size(); i++) {
            Map<String, Object> row = batch.get(i);
            int rowNum = startIdx + i + 1;

            try {
                // Obtener valor del campo de enlace
                Object linkValue = getValueFromRowData(row, linkField);
                if (linkValue == null || linkValue.toString().trim().isEmpty()) {
                    errors.add("Fila " + rowNum + ": Campo de enlace vacío");
                    continue;
                }

                // Preparar valores para el UPDATE
                Object[] args = new Object[columnsToUpdate.size() + 1];
                int idx = 0;

                for (ColumnUpdateInfo col : columnsToUpdate) {
                    Object value = getValueFromRowData(row, col.originalName);
                    args[idx++] = convertValueForUpdate(value, col.header);
                }

                // Último parámetro: valor del campo de enlace para WHERE
                args[idx] = linkValue.toString().trim();
                batchArgs.add(args);

            } catch (Exception e) {
                errors.add("Fila " + rowNum + ": " + e.getMessage());
            }
        }

        if (batchArgs.isEmpty()) {
            return new int[0];
        }

        // Ejecutar batch update
        return jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    /**
     * Construye el resultado de la operación complementaria
     */
    private Map<String, Object> buildComplementaryResult(int totalRows, int updatedRows, int notFoundRows,
                                                          int failedRows, String tableName, List<String> errors,
                                                          long startTime) {
        Map<String, Object> result = new HashMap<>();
        result.put("totalRows", totalRows);
        result.put("updatedRows", updatedRows);
        result.put("notFoundRows", notFoundRows);
        result.put("failedRows", failedRows);
        result.put("tableName", tableName);
        result.put("durationMs", System.currentTimeMillis() - startTime);

        if (!errors.isEmpty()) {
            result.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);
            result.put("totalErrors", errors.size());
        }

        return result;
    }

    /**
     * Verifica si una columna existe en la tabla
     */
    private boolean columnExists(String tableName, String columnName) {
        try {
            String safeTableName = validateAndSanitizeTableName(tableName);
            String safeColumnName = sanitizeColumnName(columnName);

            String sql = "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, safeTableName, safeColumnName);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Error al verificar existencia de columna {}.{}: {}", tableName, columnName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Convierte un valor según la configuración de cabecera (para UPDATE)
     * Usa DataTypeValidator para validación consistente
     */
    private Object convertValueForUpdate(Object value, HeaderConfiguration header) {
        if (value == null) return null;

        // Si no hay header, devolver el valor como string
        if (header == null) {
            return value.toString();
        }

        String dataType = header.getDataType();
        if (dataType == null) {
            return value.toString();
        }

        // Usar DataTypeValidator para validar y convertir (required=false para updates)
        DataTypeValidator.ValidationResult result = DataTypeValidator.validate(
            value,
            dataType,
            header.getFormat(),
            header.getHeaderName(),
            false  // No requerir para updates parciales
        );

        if (!result.isValid()) {
            logger.warn("Error validando valor para campo '{}': {}", header.getHeaderName(), result.getErrorMessage());
            return null;
        }

        return result.getConvertedValue();
    }

    /**
     * Detecta automáticamente el linkField basándose en las cabeceras configuradas
     * Busca campos como identity_code, cod_cli, codigo_identificacion
     */
    private String detectLinkField(List<HeaderConfiguration> headers) {
        // Lista de nombres de campo comunes para identificación de clientes
        List<String> identityFieldNames = List.of(
            "identity_code", "codigo_identificacion", "cod_cli", "customer_id",
            "id_cliente", "documento", "dni", "ruc", "num_documento"
        );

        for (String fieldName : identityFieldNames) {
            String columnName = findIdentityColumn(headers, fieldName);
            if (columnName != null) {
                logger.info("LinkField detectado automáticamente: {}", columnName);
                return columnName;
            }
        }

        // Si no se encuentra, usar el primer campo como fallback
        if (!headers.isEmpty()) {
            String fallback = sanitizeColumnName(headers.get(0).getHeaderName());
            logger.warn("No se detectó campo de identificación, usando fallback: {}", fallback);
            return fallback;
        }

        return null;
    }

    // ==================== CARGA DIARIA ====================

    /**
     * Importa datos de carga diaria.
     * Esta operación:
     * 1. Inserta/Actualiza datos en la tabla ACTUALIZACION (histórico diario)
     * 2. Actualiza los registros correspondientes en la tabla INICIAL (tabla maestra) usando el linkField
     * 3. Sincroniza los clientes SOLO desde la tabla INICIAL
     *
     * @param subPortfolioId ID de la subcartera
     * @param data Lista de registros a importar
     * @param linkField Campo de enlace para vincular registros con la tabla inicial
     * @return Mapa con estadísticas de la operación
     */
    @Override
    @Transactional
    public Map<String, Object> importDailyData(Integer subPortfolioId, List<Map<String, Object>> data, String linkField) {
        logger.info("📅 Iniciando carga diaria para SubPortfolio ID: {}, registros: {}, linkField: {}", subPortfolioId, data.size(), linkField);

        // Validar linkField
        if (linkField == null || linkField.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo de enlace (linkField) es requerido para la carga diaria.");
        }

        // Validar que la subcartera existe
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        String tableActualizacion = buildTableName(subPortfolio, LoadType.ACTUALIZACION);
        String tableInicial = buildTableName(subPortfolio, LoadType.INICIAL);

        // Verificar que ambas tablas existen
        if (!dynamicTableExists(tableActualizacion)) {
            throw new IllegalArgumentException("La tabla de actualización no existe: " + tableActualizacion + ". Debe configurar las cabeceras de actualización primero.");
        }
        if (!dynamicTableExists(tableInicial)) {
            throw new IllegalArgumentException("La tabla inicial no existe: " + tableInicial + ". Debe realizar primero una carga inicial de mes.");
        }

        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        // ========== FASE 1: Importar a tabla ACTUALIZACION ==========
        logger.info("📊 Fase 1: Importando a tabla ACTUALIZACION ({})", tableActualizacion);
        Map<String, Object> actualizacionResult;
        try {
            actualizacionResult = importDataToTable(subPortfolioId, LoadType.ACTUALIZACION, data);
            result.put("actualizacion", actualizacionResult);
            logger.info("✅ Fase 1 completada: {} insertados, {} actualizados en tabla ACTUALIZACION",
                    actualizacionResult.get("insertedRows"), actualizacionResult.get("updatedRows"));
        } catch (Exception e) {
            logger.error("❌ Error en Fase 1 (ACTUALIZACION): {}", e.getMessage(), e);
            errors.add("Error importando a tabla de actualización: " + e.getMessage());
            result.put("actualizacionError", e.getMessage());
            actualizacionResult = Map.of("insertedRows", 0, "updatedRows", 0);
        }

        // ========== FASE 2: Actualizar tabla INICIAL ==========
        logger.info("📊 Fase 2: Actualizando tabla INICIAL ({})", tableInicial);

        // Obtener cabeceras de INICIAL para el mapeo
        List<HeaderConfiguration> headersInicial = headerConfigurationRepository.findBySubPortfolioAndLoadType(subPortfolio, LoadType.INICIAL);
        if (headersInicial.isEmpty()) {
            throw new IllegalArgumentException("No hay cabeceras configuradas para carga inicial.");
        }

        // Usar el campo de enlace proporcionado por el usuario
        // Sanitizar el nombre del campo para que coincida con el formato de las columnas en la tabla
        String sanitizedLinkField = sanitizeColumnName(linkField);
        logger.info("🔗 Campo de enlace proporcionado: {} (sanitizado: {})", linkField, sanitizedLinkField);

        // Actualizar registros en tabla INICIAL usando el campo de enlace
        int updatedInInicial = 0;
        int notFoundInInicial = 0;
        int failedInInicial = 0;

        // Obtener cabeceras de ACTUALIZACION para mapear los datos de entrada
        List<HeaderConfiguration> headersActualizacion = headerConfigurationRepository.findBySubPortfolioAndLoadType(subPortfolio, LoadType.ACTUALIZACION);

        // Crear mapa de nombre de columna sanitizado a HeaderConfiguration para INICIAL
        Map<String, HeaderConfiguration> inicialHeaderMap = new HashMap<>();
        for (HeaderConfiguration h : headersInicial) {
            inicialHeaderMap.put(sanitizeColumnName(h.getHeaderName()).toLowerCase(), h);
            // También mapear por sourceField si existe
            if (h.getSourceField() != null && !h.getSourceField().trim().isEmpty()) {
                inicialHeaderMap.put(h.getSourceField().toLowerCase().trim(), h);
            }
        }

        // ========== BATCH UPDATE OPTIMIZADO ==========
        // Paso 1: Determinar todas las columnas que se pueden actualizar en INICIAL
        Set<String> columnsToUpdateSet = new LinkedHashSet<>();
        for (Map<String, Object> row : data) {
            for (String key : row.keySet()) {
                String columnKey = key.toLowerCase().trim();
                HeaderConfiguration inicialHeader = inicialHeaderMap.get(columnKey);
                if (inicialHeader == null) {
                    inicialHeader = inicialHeaderMap.get(sanitizeColumnName(columnKey).toLowerCase());
                }
                if (inicialHeader != null) {
                    String columnName = sanitizeColumnName(inicialHeader.getHeaderName());
                    if (!columnName.equalsIgnoreCase(sanitizedLinkField)) {
                        columnsToUpdateSet.add(columnName);
                    }
                }
            }
        }

        if (columnsToUpdateSet.isEmpty()) {
            logger.warn("⚠️ No hay columnas comunes entre los datos y la tabla INICIAL");
            result.put("inicial", Map.of("updatedRows", 0, "notFoundRows", data.size(), "failedRows", 0));
        } else {
            // Paso 2: Construir SQL template para batch update
            List<String> columnsList = new ArrayList<>(columnsToUpdateSet);
            StringBuilder setClause = new StringBuilder();
            for (int i = 0; i < columnsList.size(); i++) {
                if (i > 0) setClause.append(", ");
                // Usar COALESCE para mantener valor existente si el nuevo es null
                setClause.append(columnsList.get(i)).append(" = COALESCE(?, ").append(columnsList.get(i)).append(")");
            }

            String updateSql = "UPDATE " + tableInicial + " SET " + setClause +
                              " WHERE " + sanitizedLinkField + " = ?";

            logger.info("📝 SQL de actualización batch: {} columnas, procesando en batches de {}",
                       columnsList.size(), BATCH_SIZE_FOR_DAILY_UPDATE);

            // Paso 3: Preparar datos para batch update
            List<Object[]> batchArgs = new ArrayList<>();
            List<Integer> rowIndices = new ArrayList<>(); // Para tracking de errores
            List<String> linkValuesInOrder = new ArrayList<>(); // Para rastrear qué registros se actualizaron

            for (int i = 0; i < data.size(); i++) {
                Map<String, Object> row = data.get(i);
                try {
                    String linkValue = extractLinkValue(row, linkField, headersActualizacion);
                    if (linkValue == null || linkValue.trim().isEmpty()) {
                        failedInInicial++;
                        errors.add("Fila " + (i + 1) + ": Campo de enlace vacío");
                        continue;
                    }

                    // Preparar valores para cada columna
                    Object[] args = new Object[columnsList.size() + 1];
                    for (int j = 0; j < columnsList.size(); j++) {
                        String colName = columnsList.get(j);
                        // Buscar el header correspondiente para obtener regex y tipo de dato
                        HeaderConfiguration header = findHeaderByColumnName(inicialHeaderMap, colName);

                        // Buscar el valor usando sourceField si está configurado, si no usar nombre de columna directo
                        Object value = null;
                        if (header != null && header.getSourceField() != null && !header.getSourceField().trim().isEmpty()) {
                            // Buscar por sourceField primero (ej: IDENTITY_CODE -> documento)
                            value = findValueBySourceField(row, header.getSourceField());
                        }
                        if (value == null) {
                            value = findValueForColumn(row, colName, inicialHeaderMap);
                        }

                        // Aplicar regex si está configurado (para transformar D000080413598 -> 80413598)
                        if (value != null && header != null && header.getRegexPattern() != null && !header.getRegexPattern().trim().isEmpty()) {
                            String sourceStr = value.toString();
                            value = applyRegexTransformation(sourceStr, header.getRegexPattern(), header.getHeaderName());
                            logger.debug("🔄 Regex aplicado para {}: {} -> {}", colName, sourceStr, value);
                        }

                        // Convertir al tipo de dato correcto
                        args[j] = (value != null && header != null) ? convertValueForUpdate(value, header) : null;
                    }
                    args[columnsList.size()] = linkValue; // WHERE clause

                    batchArgs.add(args);
                    rowIndices.add(i);
                    linkValuesInOrder.add(linkValue); // Guardar el linkValue en orden

                } catch (Exception e) {
                    failedInInicial++;
                    errors.add("Fila " + (i + 1) + " (preparación): " + e.getMessage());
                }
            }

            // Paso 4: Ejecutar batch updates y recolectar IDs actualizados
            logger.info("🚀 Ejecutando {} actualizaciones en batches de {}...",
                       batchArgs.size(), BATCH_SIZE_FOR_DAILY_UPDATE);

            int totalBatches = (int) Math.ceil((double) batchArgs.size() / BATCH_SIZE_FOR_DAILY_UPDATE);
            int processedBatches = 0;
            Set<String> updatedIdentificationCodes = new HashSet<>(); // Códigos de identificación actualizados

            for (int batchStart = 0; batchStart < batchArgs.size(); batchStart += BATCH_SIZE_FOR_DAILY_UPDATE) {
                int batchEnd = Math.min(batchStart + BATCH_SIZE_FOR_DAILY_UPDATE, batchArgs.size());
                List<Object[]> currentBatch = batchArgs.subList(batchStart, batchEnd);

                try {
                    int[] results = jdbcTemplate.batchUpdate(updateSql, currentBatch);

                    // Contar resultados y recolectar IDs actualizados
                    for (int k = 0; k < results.length; k++) {
                        if (results[k] > 0) {
                            updatedInInicial += results[k];
                            // Agregar el linkValue del registro actualizado exitosamente
                            int globalIndex = batchStart + k;
                            if (globalIndex < linkValuesInOrder.size()) {
                                updatedIdentificationCodes.add(linkValuesInOrder.get(globalIndex));
                            }
                        } else if (results[k] == 0) {
                            notFoundInInicial++;
                        }
                    }

                    processedBatches++;
                    if (processedBatches % 10 == 0 || processedBatches == totalBatches) {
                        logger.info("📊 Progreso: {}/{} batches procesados ({} registros)",
                                   processedBatches, totalBatches, batchEnd);
                    }

                } catch (Exception e) {
                    logger.error("❌ Error en batch {}-{}: {}", batchStart, batchEnd, e.getMessage());
                    // En caso de error de batch, marcar todas las filas como fallidas
                    failedInInicial += currentBatch.size();
                    errors.add("Error en batch " + (processedBatches + 1) + ": " + e.getMessage());
                }
            }

            logger.info("📊 Códigos de identificación actualizados exitosamente: {}", updatedIdentificationCodes.size());
            result.put("updatedIdentificationCodes", updatedIdentificationCodes);

            result.put("inicial", Map.of(
                    "updatedRows", updatedInInicial,
                    "notFoundRows", notFoundInInicial,
                    "failedRows", failedInInicial
            ));
        }

        logger.info("✅ Fase 2 completada: {} actualizados, {} no encontrados, {} fallidos en tabla INICIAL",
                   updatedInInicial, notFoundInInicial, failedInInicial);

        result.put("inicial", Map.of(
                "updatedRows", updatedInInicial,
                "notFoundRows", notFoundInInicial,
                "failedRows", failedInInicial
        ));

        // ========== FASE 3: Sincronizar SOLO los clientes actualizados ==========
        @SuppressWarnings("unchecked")
        Set<String> updatedCodes = (Set<String>) result.getOrDefault("updatedIdentificationCodes", new HashSet<>());
        logger.info("📊 Fase 3: Sincronizando {} clientes actualizados desde tabla INICIAL", updatedCodes.size());

        if (!updatedCodes.isEmpty()) {
            try {
                // Sincronizar SOLO los clientes que fueron actualizados (sincronización selectiva)
                CustomerSyncService.SyncResult syncResult = customerSyncService.syncCustomersByIdentificationCodes(
                        subPortfolioId.longValue(),
                        LoadType.INICIAL,  // Siempre sincronizar desde INICIAL
                        updatedCodes       // Solo los códigos actualizados
                );
                logger.info("✅ Sincronización selectiva completada: {} clientes creados, {} actualizados",
                        syncResult.getCustomersCreated(), syncResult.getCustomersUpdated());

                result.put("syncCustomersCreated", syncResult.getCustomersCreated());
                result.put("syncCustomersUpdated", syncResult.getCustomersUpdated());
                if (syncResult.hasErrors()) {
                    result.put("syncErrors", syncResult.getErrors());
                }
            } catch (Exception e) {
                logger.error("❌ Error en sincronización de clientes: {}", e.getMessage(), e);
                result.put("syncError", "Error al sincronizar clientes: " + e.getMessage());
            }
        } else {
            logger.info("⚠️ No hay clientes para sincronizar (ningún registro actualizado en INICIAL)");
            result.put("syncCustomersCreated", 0);
            result.put("syncCustomersUpdated", 0);
        }

        // Limpiar el set temporal del resultado
        result.remove("updatedIdentificationCodes");

        // Preparar resumen
        result.put("totalRows", data.size());
        result.put("tableActualizacion", tableActualizacion);
        result.put("tableInicial", tableInicial);

        if (!errors.isEmpty()) {
            result.put("errors", errors.size() > 20 ? errors.subList(0, 20) : errors);
            result.put("totalErrors", errors.size());
        }

        logger.info("📅 Carga diaria completada para SubPortfolio ID: {}", subPortfolioId);
        return result;
    }

    /**
     * Extrae el valor del campo de enlace de una fila
     */
    private String extractLinkValue(Map<String, Object> row, String linkField, List<HeaderConfiguration> headers) {
        // Buscar directamente en el row
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase(linkField) ||
                sanitizeColumnName(key).equalsIgnoreCase(linkField)) {
                Object value = entry.getValue();
                return value != null ? value.toString().trim() : null;
            }
        }

        // Buscar por sourceField en las cabeceras
        for (HeaderConfiguration header : headers) {
            if (sanitizeColumnName(header.getHeaderName()).equalsIgnoreCase(linkField)) {
                String sourceField = header.getSourceField();
                if (sourceField != null && !sourceField.trim().isEmpty()) {
                    Object value = row.get(sourceField);
                    if (value == null) {
                        // Buscar case-insensitive
                        for (Map.Entry<String, Object> entry : row.entrySet()) {
                            if (entry.getKey().equalsIgnoreCase(sourceField)) {
                                value = entry.getValue();
                                break;
                            }
                        }
                    }
                    return value != null ? value.toString().trim() : null;
                }
            }
        }

        return null;
    }

    /**
     * Busca el valor de una columna en una fila de datos.
     * Intenta múltiples variaciones del nombre de columna (original, sanitizado, case-insensitive).
     */
    private Object findValueForColumn(Map<String, Object> row, String columnName,
                                       Map<String, HeaderConfiguration> headerMap) {
        // Buscar directamente
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            String sanitizedKey = sanitizeColumnName(key);

            if (sanitizedKey.equalsIgnoreCase(columnName)) {
                return entry.getValue();
            }
        }

        // Buscar por sourceField en las cabeceras
        for (Map.Entry<String, HeaderConfiguration> headerEntry : headerMap.entrySet()) {
            HeaderConfiguration header = headerEntry.getValue();
            if (sanitizeColumnName(header.getHeaderName()).equalsIgnoreCase(columnName)) {
                String sourceField = header.getSourceField();
                if (sourceField != null && !sourceField.trim().isEmpty()) {
                    // Buscar el valor usando sourceField
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(sourceField) ||
                            entry.getKey().equalsIgnoreCase(header.getHeaderName())) {
                            return entry.getValue();
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Busca un valor en la fila usando el sourceField especificado.
     * Útil para campos transformados donde el sourceField indica la columna origen (ej: IDENTITY_CODE -> documento)
     */
    private Object findValueBySourceField(Map<String, Object> row, String sourceField) {
        if (sourceField == null || sourceField.trim().isEmpty()) {
            return null;
        }

        // Buscar el valor directamente por sourceField
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase(sourceField) ||
                key.equalsIgnoreCase(sourceField.trim()) ||
                sanitizeColumnName(key).equalsIgnoreCase(sanitizeColumnName(sourceField))) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Busca la configuración de cabecera por nombre de columna sanitizado.
     */
    private HeaderConfiguration findHeaderByColumnName(Map<String, HeaderConfiguration> headerMap, String columnName) {
        for (Map.Entry<String, HeaderConfiguration> entry : headerMap.entrySet()) {
            HeaderConfiguration header = entry.getValue();
            if (sanitizeColumnName(header.getHeaderName()).equalsIgnoreCase(columnName)) {
                return header;
            }
        }
        return null;
    }

    // ==================== Import from SubPortfolio ====================

    @Override
    @Transactional(readOnly = true)
    public ImportPreviewResult previewImportFromSubPortfolio(Integer targetSubPortfolioId,
                                                              Integer sourceSubPortfolioId,
                                                              LoadType loadType) {
        // Validaciones
        if (targetSubPortfolioId == null || sourceSubPortfolioId == null) {
            throw new IllegalArgumentException("Los IDs de subcartera origen y destino son obligatorios");
        }
        if (loadType == null) {
            throw new IllegalArgumentException("El tipo de carga es obligatorio");
        }
        if (targetSubPortfolioId.equals(sourceSubPortfolioId)) {
            throw new IllegalArgumentException("La subcartera origen y destino no pueden ser la misma");
        }

        // Obtener subcarteras
        SubPortfolio sourceSubPortfolio = subPortfolioRepository.findById(sourceSubPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera origen no encontrada: " + sourceSubPortfolioId));
        SubPortfolio targetSubPortfolio = subPortfolioRepository.findById(targetSubPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera destino no encontrada: " + targetSubPortfolioId));

        // Obtener cabeceras de origen y destino
        List<HeaderConfiguration> sourceHeaders = headerConfigurationRepository
                .findBySubPortfolioAndLoadType(sourceSubPortfolio, loadType);
        List<HeaderConfiguration> targetHeaders = headerConfigurationRepository
                .findBySubPortfolioAndLoadType(targetSubPortfolio, loadType);

        // Crear set de nombres de cabeceras destino para búsqueda rápida
        Set<String> targetHeaderNames = new HashSet<>();
        Map<String, HeaderConfiguration> targetHeaderMap = new HashMap<>();
        for (HeaderConfiguration header : targetHeaders) {
            String normalizedName = header.getHeaderName().toLowerCase().trim();
            targetHeaderNames.add(normalizedName);
            targetHeaderMap.put(normalizedName, header);
        }

        // Clasificar cabeceras
        List<HeaderPreviewItem> headersToImport = new ArrayList<>();
        List<ConflictItem> conflicts = new ArrayList<>();

        for (HeaderConfiguration sourceHeader : sourceHeaders) {
            String normalizedName = sourceHeader.getHeaderName().toLowerCase().trim();
            int aliasCount = sourceHeader.getAliases() != null ? sourceHeader.getAliases().size() : 0;

            if (targetHeaderNames.contains(normalizedName)) {
                // Conflicto: existe en destino
                HeaderConfiguration targetHeader = targetHeaderMap.get(normalizedName);
                conflicts.add(new ConflictItem(
                        sourceHeader.getHeaderName(),
                        sourceHeader.getDisplayLabel(),
                        targetHeader.getDisplayLabel()
                ));
            } else {
                // Nueva cabecera a importar
                headersToImport.add(new HeaderPreviewItem(
                        sourceHeader.getHeaderName(),
                        sourceHeader.getDataType(),
                        sourceHeader.getDisplayLabel(),
                        aliasCount > 0,
                        aliasCount
                ));
            }
        }

        return new ImportPreviewResult(
                sourceSubPortfolioId,
                sourceSubPortfolio.getSubPortfolioName(),
                targetSubPortfolioId,
                targetSubPortfolio.getSubPortfolioName(),
                loadType,
                headersToImport,
                conflicts,
                headersToImport.size(),
                conflicts.size()
        );
    }

    @Override
    @Transactional
    public ImportResult importFromSubPortfolio(Integer targetSubPortfolioId,
                                                Integer sourceSubPortfolioId,
                                                LoadType loadType,
                                                String conflictResolution,
                                                List<String> headersToReplace) {
        // Validaciones
        if (targetSubPortfolioId == null || sourceSubPortfolioId == null) {
            throw new IllegalArgumentException("Los IDs de subcartera origen y destino son obligatorios");
        }
        if (loadType == null) {
            throw new IllegalArgumentException("El tipo de carga es obligatorio");
        }
        if (conflictResolution == null || conflictResolution.trim().isEmpty()) {
            throw new IllegalArgumentException("La resolución de conflictos es obligatoria");
        }
        if (targetSubPortfolioId.equals(sourceSubPortfolioId)) {
            throw new IllegalArgumentException("La subcartera origen y destino no pueden ser la misma");
        }

        // Normalizar conflictResolution
        String resolution = conflictResolution.toUpperCase().trim();
        if (!resolution.equals("SKIP") && !resolution.equals("REPLACE") && !resolution.equals("SELECTIVE")) {
            throw new IllegalArgumentException("Resolución de conflictos inválida: " + conflictResolution + ". Use SKIP, REPLACE o SELECTIVE");
        }

        // Obtener subcarteras
        SubPortfolio sourceSubPortfolio = subPortfolioRepository.findById(sourceSubPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera origen no encontrada: " + sourceSubPortfolioId));
        SubPortfolio targetSubPortfolio = subPortfolioRepository.findById(targetSubPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera destino no encontrada: " + targetSubPortfolioId));

        // Obtener cabeceras de origen y destino
        List<HeaderConfiguration> sourceHeaders = headerConfigurationRepository
                .findBySubPortfolioAndLoadType(sourceSubPortfolio, loadType);
        List<HeaderConfiguration> targetHeaders = headerConfigurationRepository
                .findBySubPortfolioAndLoadType(targetSubPortfolio, loadType);

        // Crear map de cabeceras destino
        Map<String, HeaderConfiguration> targetHeaderMap = new HashMap<>();
        for (HeaderConfiguration header : targetHeaders) {
            targetHeaderMap.put(header.getHeaderName().toLowerCase().trim(), header);
        }

        // Crear set de headers a reemplazar (normalizado)
        Set<String> headersToReplaceSet = new HashSet<>();
        if (headersToReplace != null) {
            for (String h : headersToReplace) {
                headersToReplaceSet.add(h.toLowerCase().trim());
            }
        }

        int headersImported = 0;
        int headersSkipped = 0;
        int headersReplaced = 0;
        int aliasesImported = 0;
        List<String> errors = new ArrayList<>();

        for (HeaderConfiguration sourceHeader : sourceHeaders) {
            String normalizedName = sourceHeader.getHeaderName().toLowerCase().trim();
            HeaderConfiguration existingTarget = targetHeaderMap.get(normalizedName);

            try {
                if (existingTarget != null) {
                    // Hay conflicto
                    boolean shouldReplace = false;

                    if (resolution.equals("REPLACE")) {
                        shouldReplace = true;
                    } else if (resolution.equals("SELECTIVE")) {
                        shouldReplace = headersToReplaceSet.contains(normalizedName);
                    }
                    // SKIP: shouldReplace = false

                    if (shouldReplace) {
                        // Eliminar cabecera existente (cascade elimina aliases)
                        headerConfigurationRepository.delete(existingTarget);
                        headerConfigurationRepository.flush();

                        // Copiar cabecera de origen
                        int aliasesCopied = copyHeaderConfiguration(sourceHeader, targetSubPortfolio, loadType);
                        headersReplaced++;
                        aliasesImported += aliasesCopied;
                    } else {
                        headersSkipped++;
                    }
                } else {
                    // No hay conflicto, copiar directamente
                    int aliasesCopied = copyHeaderConfiguration(sourceHeader, targetSubPortfolio, loadType);
                    headersImported++;
                    aliasesImported += aliasesCopied;
                }
            } catch (Exception e) {
                errors.add("Error al importar cabecera '" + sourceHeader.getHeaderName() + "': " + e.getMessage());
                logger.error("Error al importar cabecera '{}': {}", sourceHeader.getHeaderName(), e.getMessage());
            }
        }

        logger.info("Importación de cabeceras completada: {} importadas, {} reemplazadas, {} omitidas, {} aliases, {} errores",
                headersImported, headersReplaced, headersSkipped, aliasesImported, errors.size());

        return new ImportResult(
                errors.isEmpty(),
                headersImported,
                headersSkipped,
                headersReplaced,
                aliasesImported,
                errors
        );
    }

    /**
     * Copia una configuración de cabecera de origen a la subcartera destino, incluyendo aliases.
     *
     * @param sourceHeader Cabecera origen a copiar
     * @param targetSubPortfolio Subcartera destino
     * @param loadType Tipo de carga
     * @return Número de aliases copiados
     */
    private int copyHeaderConfiguration(HeaderConfiguration sourceHeader, SubPortfolio targetSubPortfolio, LoadType loadType) {
        HeaderConfiguration newHeader;

        if (sourceHeader.getFieldDefinition() != null) {
            // Cabecera vinculada al catálogo
            newHeader = new HeaderConfiguration(
                    targetSubPortfolio,
                    sourceHeader.getFieldDefinition(),
                    sourceHeader.getHeaderName(),
                    sourceHeader.getDisplayLabel(),
                    sourceHeader.getFormat(),
                    sourceHeader.getRequired(),
                    loadType
            );
        } else {
            // Cabecera personalizada
            newHeader = new HeaderConfiguration(
                    targetSubPortfolio,
                    sourceHeader.getHeaderName(),
                    sourceHeader.getDataType(),
                    sourceHeader.getDisplayLabel(),
                    sourceHeader.getFormat(),
                    sourceHeader.getRequired(),
                    loadType
            );
        }

        // Copiar campos de transformación
        newHeader.setSourceField(sourceHeader.getSourceField());
        newHeader.setRegexPattern(sourceHeader.getRegexPattern());

        // Guardar la cabecera
        HeaderConfiguration savedHeader = headerConfigurationRepository.save(newHeader);

        // Copiar aliases
        int aliasesCopied = 0;
        if (sourceHeader.getAliases() != null && !sourceHeader.getAliases().isEmpty()) {
            for (var sourceAlias : sourceHeader.getAliases()) {
                savedHeader.addAlias(sourceAlias.getAlias(), sourceAlias.isPrincipal());
                aliasesCopied++;
            }
            // Guardar con aliases
            headerConfigurationRepository.save(savedHeader);
        }

        return aliasesCopied;
    }
}
