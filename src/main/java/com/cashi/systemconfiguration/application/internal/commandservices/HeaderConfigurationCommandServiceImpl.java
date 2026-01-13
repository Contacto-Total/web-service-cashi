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
import com.cashi.systemconfiguration.domain.services.HeaderConfigurationCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public HeaderConfigurationCommandServiceImpl(
            HeaderConfigurationRepository headerConfigurationRepository,
            SubPortfolioRepository subPortfolioRepository,
            FieldDefinitionRepository fieldDefinitionRepository,
            JdbcTemplate jdbcTemplate,
            CustomerRepository customerRepository,
            ContactMethodRepository contactMethodRepository,
            CustomerSyncService customerSyncService) {
        this.headerConfigurationRepository = headerConfigurationRepository;
        this.subPortfolioRepository = subPortfolioRepository;
        this.fieldDefinitionRepository = fieldDefinitionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.customerRepository = customerRepository;
        this.contactMethodRepository = contactMethodRepository;
        this.customerSyncService = customerSyncService;
    }

    @Override
    @Transactional
    public HeaderConfiguration createHeaderConfiguration(Integer subPortfolioId, Integer fieldDefinitionId,
                                                         String headerName, String displayLabel,
                                                         String format, Boolean required, LoadType loadType) {
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
        if (fieldDefinitionId == null) {
            throw new IllegalArgumentException("El ID de definición de campo es obligatorio. Para campos personalizados use el endpoint /bulk");
        }

        // Validar que la subcartera existe
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        String tableName = buildTableName(subPortfolio, loadType);

        // Validar que la definición de campo existe
        FieldDefinition fieldDefinition = fieldDefinitionRepository.findById(fieldDefinitionId)
                .orElseThrow(() -> new IllegalArgumentException("Definición de campo no encontrada con ID: " + fieldDefinitionId));

        // Validar que el nombre de cabecera no exista para esta subcartera y tipo de carga
        if (headerConfigurationRepository.existsBySubPortfolioAndHeaderNameAndLoadType(subPortfolio, headerName, loadType)) {
            throw new IllegalArgumentException("Ya existe una cabecera con el nombre: " + headerName + " para esta subcartera y tipo de carga");
        }

        // Crear la configuración
        HeaderConfiguration headerConfig = new HeaderConfiguration(
                subPortfolio, fieldDefinition, headerName, displayLabel, format,
                required != null ? (required ? 1 : 0) : 0, loadType
        );

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

        // Verificar si la tabla tiene datos (operación destructiva, se perderían datos de la columna)
        if (dynamicTableExists(tableName) && hasDataInTable(tableName)) {
            throw new IllegalArgumentException(
                "No se puede eliminar la cabecera '" + headerConfig.getHeaderName() +
                "' porque la tabla ya contiene datos. Debe eliminar los datos primero."
            );
        }

        // Eliminar la columna de la tabla si existe
        if (dynamicTableExists(tableName)) {
            dropColumnFromTable(tableName, headerConfig);
        }

        headerConfigurationRepository.delete(headerConfig);
    }

    @Override
    @Transactional
    public List<HeaderConfiguration> createBulkHeaderConfigurations(Integer subPortfolioId,
                                                                    LoadType loadType,
                                                                    List<HeaderConfigurationData> headers) {
        // Validar que la subcartera existe
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

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
                if (!List.of("TEXTO", "NUMERICO", "FECHA").contains(data.dataType().toUpperCase())) {
                    throw new IllegalArgumentException("DataType inválido para campo personalizado: " + data.dataType() + ". Valores válidos: TEXTO, NUMERICO, FECHA");
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

            createdConfigs.add(headerConfigurationRepository.save(headerConfig));
        }

        // Crear tabla dinámica después de guardar todas las configuraciones
        createDynamicTableForSubPortfolio(subPortfolio, loadType, createdConfigs);

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
     */
    private String sanitizeColumnName(String headerName) {
        return headerName
                .toLowerCase()
                .replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("[ñ]", "n")
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
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

        // Verificar si la tabla tiene datos (operación destructiva, se perderían todos los datos)
        if (dynamicTableExists(tableName) && hasDataInTable(tableName)) {
            throw new IllegalArgumentException(
                "No se pueden eliminar las configuraciones porque la tabla '" + tableName +
                "' ya contiene datos. Debe eliminar los datos primero."
            );
        }

        // Eliminar la tabla dinámica completa si existe
        if (dynamicTableExists(tableName)) {
            dropDynamicTable(tableName);
        }

        headerConfigurationRepository.deleteBySubPortfolioAndLoadType(subPortfolio, loadType);
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
     * INICIAL: ini_<codproveedor>_<codcartera>_<codsubcartera>
     * ACTUALIZACION: <codproveedor>_<codcartera>_<codsubcartera> (sin prefijo)
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
            String sql = "SELECT COUNT(*) FROM " + tableName;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Error al verificar datos en tabla {}: {}", tableName, e.getMessage());
            return false;
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

        // ========== OPTIMIZACIÓN: BATCH INSERT ==========
        // Preparar todas las filas válidas para batch insert
        List<PreparedRowData> validRows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int failedRows = 0;

        logger.info("📦 Preparando {} filas para batch insert...", data.size());

        // Fase 1: Validar y preparar todas las filas
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                PreparedRowData preparedRow = prepareRowData(row, headers, i + 1);
                validRows.add(preparedRow);
            } catch (Exception e) {
                failedRows++;
                errors.add("Fila " + (i + 1) + ": " + e.getMessage());
                // Solo loguear en DEBUG para evitar ralentización en producción
                if (logger.isDebugEnabled()) {
                    logger.debug("Error al validar fila {}: {}", i + 1, e.getMessage());
                }
            }
        }

        logger.info("✅ Filas preparadas: {} válidas, {} con errores", validRows.size(), failedRows);

        // Loguear un resumen de errores si hay muchos
        if (failedRows > 0 && logger.isDebugEnabled()) {
            logger.debug("Resumen de errores de validación: {} filas fallidas de {}", failedRows, data.size());
        }

        int insertedRows = 0;

        // Fase 2: Insertar todas las filas válidas en un solo batch
        if (!validRows.isEmpty()) {
            try {
                insertedRows = batchInsertRows(tableName, validRows, headers);
                logger.info("✅ Batch insert completado: {} filas insertadas", insertedRows);
            } catch (Exception e) {
                logger.error("❌ Error en batch insert: {}", e.getMessage(), e);
                throw new RuntimeException("Error al insertar datos en lote: " + e.getMessage(), e);
            }
        }

        // Preparar respuesta
        Map<String, Object> result = new HashMap<>();
        result.put("totalRows", data.size());
        result.put("insertedRows", insertedRows);
        result.put("failedRows", failedRows);
        result.put("tableName", tableName);

        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        logger.info("Importación completada: {} filas insertadas, {} fallidas", insertedRows, failedRows);

        // 🔄 Sincronizar clientes desde la tabla dinámica a la tabla clientes
        if (insertedRows > 0) {
            logger.info("🔄 Iniciando sincronización de clientes para SubPortfolio ID: {}, LoadType: {}", subPortfolioId, loadType);
            try {
                CustomerSyncService.SyncResult syncResult = customerSyncService.syncCustomersFromSubPortfolio(subPortfolioId.longValue(), loadType);
                logger.info("✅ Sincronización completada: {} clientes creados, {} actualizados",
                        syncResult.getCustomersCreated(), syncResult.getCustomersUpdated());

                // Agregar información de sincronización al resultado
                result.put("syncCustomersCreated", syncResult.getCustomersCreated());
                result.put("syncCustomersUpdated", syncResult.getCustomersUpdated());
                if (syncResult.hasErrors()) {
                    result.put("syncErrors", syncResult.getErrors());
                }
            } catch (Exception e) {
                logger.error("❌ Error en sincronización de clientes: {}", e.getMessage(), e);
                result.put("syncError", "Error al sincronizar clientes: " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Clase interna para almacenar datos de fila preparada
     */
    private static class PreparedRowData {
        private final int rowNumber;
        private final List<Object> values;

        public PreparedRowData(int rowNumber, List<Object> values) {
            this.rowNumber = rowNumber;
            this.values = values;
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

            // Convertir el valor según el tipo de dato
            if (value == null) {
                values.add(null);
            } else {
                switch (header.getDataType().toUpperCase()) {
                    case "NUMERICO":
                        try {
                            if (value instanceof Number) {
                                values.add(value);
                            } else {
                                String numStr = value.toString().trim();
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
                        try {
                            if (value instanceof LocalDate) {
                                values.add(value);
                            } else if (value instanceof java.time.LocalDateTime) {
                                values.add(((java.time.LocalDateTime) value).toLocalDate());
                            } else {
                                String dateStr = value.toString().trim();
                                String format = header.getFormat();

                                if (format != null && !format.isEmpty()) {
                                    String flexibleFormat = format
                                        .replace("dd", "d")
                                        .replace("MM", "M");

                                    java.time.format.DateTimeFormatter formatter =
                                        java.time.format.DateTimeFormatter.ofPattern(flexibleFormat);

                                    if (format.contains("HH") || format.contains("hh") || format.contains("mm") || format.contains("ss")) {
                                        java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(dateStr, formatter);
                                        values.add(dateTime.toLocalDate());
                                    } else {
                                        values.add(LocalDate.parse(dateStr, formatter));
                                    }
                                } else {
                                    values.add(LocalDate.parse(dateStr));
                                }
                            }
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Valor no es fecha válida para campo " + header.getHeaderName() + ": " + value +
                                " (formato esperado: " + (header.getFormat() != null ? header.getFormat() : "yyyy-MM-dd") + ")");
                        }
                        break;
                    case "TEXTO":
                    default:
                        values.add(value.toString());
                        break;
                }
            }
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
     * Consulta qué identity_codes ya existen en la tabla
     */
    private Set<String> queryExistingIdentityCodes(String tableName, String identityCodeColumn, List<String> codes) {
        if (codes.isEmpty()) {
            return new HashSet<>();
        }

        // Construir placeholders para IN clause
        String placeholders = codes.stream().map(c -> "?").collect(java.util.stream.Collectors.joining(", "));
        String sql = "SELECT " + identityCodeColumn + " FROM " + tableName +
                     " WHERE " + identityCodeColumn + " IN (" + placeholders + ")";

        try {
            List<String> existingCodes = jdbcTemplate.queryForList(sql, String.class, codes.toArray());
            return new HashSet<>(existingCodes);
        } catch (Exception e) {
            logger.error("❌ Error al consultar identity_codes existentes: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * Batch UPDATE para registros existentes
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

        int[] updateCounts = jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                PreparedRowData rowData = rows.get(i);
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
                return rows.size();
            }
        });

        int totalUpdated = 0;
        for (int count : updateCounts) {
            if (count > 0) {
                totalUpdated += count;
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

        logger.info("🚀 Ejecutando batch insert de {} filas en tabla {}", rows.size(), tableName);

        // Usar batchUpdate con BatchPreparedStatementSetter para máximo rendimiento
        int[] updateCounts = jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                PreparedRowData rowData = rows.get(i);
                List<Object> values = rowData.getValues();

                for (int j = 0; j < values.size(); j++) {
                    ps.setObject(j + 1, values.get(j));
                }
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });

        // Sumar todos los registros insertados
        int totalInserted = 0;
        for (int count : updateCounts) {
            if (count > 0) {
                totalInserted += count;
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
                    logger.info("🔄 Transformación aplicada: {} [{}] → {} [{}]",
                               header.getSourceField(), sourceValue, header.getHeaderName(), value);
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
                        // Convertir a fecha usando el formato configurado
                        // IMPORTANTE: Siempre se guarda como LocalDate en BD (sin hora)
                        try {
                            if (value instanceof LocalDate) {
                                values.add(value);
                            } else if (value instanceof java.time.LocalDateTime) {
                                // Si viene como LocalDateTime, extraer solo la fecha
                                values.add(((java.time.LocalDateTime) value).toLocalDate());
                            } else {
                                // Parsear string usando el formato configurado en el header
                                String dateStr = value.toString().trim();
                                String format = header.getFormat();

                                if (format != null && !format.isEmpty()) {
                                    // Crear un formatter flexible que acepte días/meses de 1 o 2 dígitos
                                    // Reemplazar 'dd' por 'd' y 'MM' por 'M' para hacerlo flexible
                                    String flexibleFormat = format
                                        .replace("dd", "d")
                                        .replace("MM", "M");

                                    java.time.format.DateTimeFormatter formatter =
                                        java.time.format.DateTimeFormatter.ofPattern(flexibleFormat);

                                    // Si el formato incluye hora, parsear como LocalDateTime y extraer fecha
                                    if (format.contains("HH") || format.contains("hh") || format.contains("mm") || format.contains("ss")) {
                                        java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(dateStr, formatter);
                                        values.add(dateTime.toLocalDate()); // Convertir a LocalDate (solo fecha)
                                    } else {
                                        // Si solo es fecha, parsear directamente como LocalDate
                                        values.add(LocalDate.parse(dateStr, formatter));
                                    }
                                } else {
                                    // Si no hay formato configurado, intentar parsear como ISO (yyyy-MM-dd)
                                    values.add(LocalDate.parse(dateStr));
                                }
                            }
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Valor no es fecha válida para campo " + header.getHeaderName() + ": " + value +
                                " (formato esperado: " + (header.getFormat() != null ? header.getFormat() : "yyyy-MM-dd") + ")" +
                                " - Error: " + e.getMessage());
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

                    // Log especial para numero_cuenta_linea_prestamo
                    if ("numero_cuenta_linea_prestamo".equals(fieldCode)) {
                        logger.info("💰 [MAPEO] NUM_CUENTA encontrado: header='{}' → fieldCode='{}' = '{}'",
                                  header.getHeaderName(), fieldCode, value);
                    }
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
                logger.debug("🔍 [ACCOUNT] Buscando 'numero_cuenta_linea_prestamo' en transformedData");
                logger.debug("🔍 [ACCOUNT] Valor encontrado: '{}'", numeroCuentaLineaPrestamo);
                logger.debug("🔍 [ACCOUNT] Campos en transformedData: {}", transformedData.keySet());

                if (numeroCuentaLineaPrestamo != null && !numeroCuentaLineaPrestamo.trim().isEmpty()) {
                    newCustomer.setAccountNumber(numeroCuentaLineaPrestamo);
                    logger.info("💰 AccountNumber SETEADO: {}", numeroCuentaLineaPrestamo);
                } else {
                    logger.warn("⚠️ AccountNumber NO encontrado en transformedData para cliente: {}", identificationCode);
                }

                Customer savedCustomer = customerRepository.save(newCustomer);
                logger.info("✅ Cliente creado automáticamente: codigo_identificacion={}, documento={}, nombre={}, accountNumber={}",
                           identificationCode, documento, nombreCompleto, savedCustomer.getAccountNumber());

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
}
