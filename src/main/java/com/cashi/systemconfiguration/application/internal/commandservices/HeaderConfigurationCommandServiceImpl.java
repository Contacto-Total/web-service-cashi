package com.cashi.systemconfiguration.application.internal.commandservices;

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

    public HeaderConfigurationCommandServiceImpl(
            HeaderConfigurationRepository headerConfigurationRepository,
            SubPortfolioRepository subPortfolioRepository,
            FieldDefinitionRepository fieldDefinitionRepository,
            JdbcTemplate jdbcTemplate) {
        this.headerConfigurationRepository = headerConfigurationRepository;
        this.subPortfolioRepository = subPortfolioRepository;
        this.fieldDefinitionRepository = fieldDefinitionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public HeaderConfiguration createHeaderConfiguration(Integer subPortfolioId, Integer fieldDefinitionId,
                                                         String headerName, String displayLabel,
                                                         String format, Boolean required, LoadType loadType) {
        // Validar que la subcartera existe
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        // Verificar si la tabla dinámica tiene datos
        String tableName = buildTableName(subPortfolio, loadType);
        if (dynamicTableExists(tableName) && hasDataInTable(tableName)) {
            throw new IllegalArgumentException("No se pueden agregar cabeceras adicionales porque la tabla ya contiene datos. Debe eliminar los datos primero.");
        }

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

        // Si la tabla existe, agregar la columna
        if (dynamicTableExists(tableName)) {
            addColumnToTable(tableName, saved);
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

        // Verificar si la tabla tiene datos
        if (dynamicTableExists(tableName) && hasDataInTable(tableName)) {
            throw new IllegalArgumentException("No se pueden eliminar cabeceras porque la tabla ya contiene datos. Debe eliminar los datos primero.");
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
        ddl.append("  fecha_creacion DATE NOT NULL,\n");
        ddl.append("  fecha_actualizacion DATE,\n");

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

        // Verificar si la tabla tiene datos
        if (dynamicTableExists(tableName) && hasDataInTable(tableName)) {
            throw new IllegalArgumentException("No se pueden eliminar las configuraciones porque la tabla ya contiene datos. Debe eliminar los datos primero.");
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

        // Validar y preparar los datos
        int insertedRows = 0;
        int failedRows = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                insertRowToTable(tableName, row, headers);
                insertedRows++;
            } catch (Exception e) {
                failedRows++;
                errors.add("Fila " + (i + 1) + ": " + e.getMessage());
                logger.error("Error al insertar fila {}: {}", i + 1, e.getMessage());
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

        return result;
    }

    /**
     * Inserta una fila en la tabla dinámica
     */
    private void insertRowToTable(String tableName, Map<String, Object> rowData, List<HeaderConfiguration> headers) {
        // Construir SQL dinámico
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();

        // Agregar columnas fijas
        columns.append("fecha_creacion");
        placeholders.append("?");
        values.add(LocalDate.now());

        // Agregar columnas dinámicas
        for (HeaderConfiguration header : headers) {
            String columnName = sanitizeColumnName(header.getHeaderName());
            Object value = rowData.get(header.getHeaderName());

            columns.append(", ").append(columnName);
            placeholders.append(", ?");

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
                                values.add(Double.parseDouble(value.toString()));
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
        } catch (Exception e) {
            logger.error("Error al ejecutar INSERT en {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Error al insertar datos: " + e.getMessage(), e);
        }
    }
}
