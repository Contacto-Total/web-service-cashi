package com.cashi.systemconfiguration.application.internal.commandservices;

import com.cashi.shared.domain.model.entities.ComplementaryFileType;
import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.ComplementaryFileTypeRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.HeaderConfigurationRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
import com.cashi.systemconfiguration.domain.services.ComplementaryFileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de archivos complementarios.
 * Los archivos complementarios actualizan columnas específicas de la tabla de carga INICIAL
 * usando un campo de vinculación (típicamente IDENTITY_CODE).
 */
@Service
public class ComplementaryFileServiceImpl implements ComplementaryFileService {

    private static final Logger logger = LoggerFactory.getLogger(ComplementaryFileServiceImpl.class);

    private final ComplementaryFileTypeRepository complementaryFileTypeRepository;
    private final SubPortfolioRepository subPortfolioRepository;
    private final HeaderConfigurationRepository headerConfigurationRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ComplementaryFileServiceImpl(
            ComplementaryFileTypeRepository complementaryFileTypeRepository,
            SubPortfolioRepository subPortfolioRepository,
            HeaderConfigurationRepository headerConfigurationRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.complementaryFileTypeRepository = complementaryFileTypeRepository;
        this.subPortfolioRepository = subPortfolioRepository;
        this.headerConfigurationRepository = headerConfigurationRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> importComplementaryData(Integer subPortfolioId, Integer complementaryTypeId,
                                                        List<Map<String, Object>> data) {
        // Obtener el tipo de archivo complementario
        ComplementaryFileType fileType = complementaryFileTypeRepository.findById(complementaryTypeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de archivo complementario no encontrado con ID: " + complementaryTypeId));

        // Validar que pertenece a la subcartera correcta
        if (!fileType.getSubPortfolio().getId().equals(subPortfolioId)) {
            throw new IllegalArgumentException(
                    "El tipo de archivo complementario no pertenece a la subcartera especificada");
        }

        return processComplementaryImport(fileType, data);
    }

    @Override
    @Transactional
    public Map<String, Object> importComplementaryDataByTypeName(Integer subPortfolioId, String typeName,
                                                                  List<Map<String, Object>> data) {
        // Buscar el tipo por nombre
        ComplementaryFileType fileType = complementaryFileTypeRepository
                .findBySubPortfolioIdAndTypeName(subPortfolioId, typeName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de archivo complementario '" + typeName + "' no encontrado para la subcartera"));

        if (!fileType.isActive()) {
            throw new IllegalArgumentException(
                    "El tipo de archivo complementario '" + typeName + "' está desactivado");
        }

        return processComplementaryImport(fileType, data);
    }

    /**
     * Procesa la importación de datos complementarios
     */
    private Map<String, Object> processComplementaryImport(ComplementaryFileType fileType,
                                                            List<Map<String, Object>> data) {
        SubPortfolio subPortfolio = fileType.getSubPortfolio();
        String tableName = buildTableName(subPortfolio, LoadType.INICIAL);
        String linkField = fileType.getLinkField();
        List<String> columnsToUpdate = parseColumnsToUpdate(fileType.getColumnsToUpdate());

        logger.info("📦 Procesando archivo complementario '{}' para subcartera '{}'. " +
                    "Tabla: {}, Campo vinculación: {}, Columnas a actualizar: {}",
                fileType.getTypeName(), subPortfolio.getSubPortfolioName(),
                tableName, linkField, columnsToUpdate);

        // Verificar que la tabla existe
        if (!dynamicTableExists(tableName)) {
            throw new IllegalArgumentException(
                    "La tabla '" + tableName + "' no existe. Debe realizar primero la carga inicial.");
        }

        // Sanitizar nombres de columnas para SQL
        String linkColumnSanitized = sanitizeColumnName(linkField);
        Map<String, String> columnMapping = new HashMap<>();
        for (String col : columnsToUpdate) {
            columnMapping.put(col, sanitizeColumnName(col));
        }

        // Verificar que las columnas existen en la tabla
        verifyColumnsExistInTable(tableName, linkColumnSanitized, columnMapping.values());

        // Procesar datos
        int updatedRows = 0;
        int notFoundRows = 0;
        int errorRows = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            try {
                // Obtener valor del campo de vinculación
                Object linkValue = getValueFromRowData(row, linkField);
                if (linkValue == null || linkValue.toString().trim().isEmpty()) {
                    errors.add("Fila " + (i + 1) + ": Campo de vinculación '" + linkField + "' vacío");
                    errorRows++;
                    continue;
                }

                // Construir UPDATE dinámico
                StringBuilder setClause = new StringBuilder();
                List<Object> values = new ArrayList<>();
                boolean first = true;

                for (String originalCol : columnsToUpdate) {
                    Object value = getValueFromRowData(row, originalCol);
                    String sanitizedCol = columnMapping.get(originalCol);

                    if (!first) {
                        setClause.append(", ");
                    }
                    setClause.append(sanitizedCol).append(" = ?");
                    values.add(value);
                    first = false;
                }

                // Agregar condición WHERE
                values.add(linkValue.toString());

                String sql = "UPDATE " + tableName + " SET " + setClause +
                        " WHERE " + linkColumnSanitized + " = ?";

                int affected = jdbcTemplate.update(sql, values.toArray());

                if (affected > 0) {
                    updatedRows += affected;
                } else {
                    notFoundRows++;
                    if (notFoundRows <= 10) { // Solo loguear los primeros 10
                        logger.debug("Registro no encontrado para {}: {}", linkField, linkValue);
                    }
                }

            } catch (Exception e) {
                errorRows++;
                errors.add("Fila " + (i + 1) + ": " + e.getMessage());
                logger.debug("Error al procesar fila {}: {}", i + 1, e.getMessage());
            }
        }

        logger.info("✅ Importación complementaria '{}' completada: {} actualizados, {} no encontrados, {} errores",
                fileType.getTypeName(), updatedRows, notFoundRows, errorRows);

        // Preparar resultado
        Map<String, Object> result = new HashMap<>();
        result.put("totalRows", data.size());
        result.put("updatedRows", updatedRows);
        result.put("notFoundRows", notFoundRows);
        result.put("errorRows", errorRows);
        result.put("tableName", tableName);
        result.put("complementaryType", fileType.getTypeName());
        result.put("columnsUpdated", columnsToUpdate);

        if (!errors.isEmpty()) {
            result.put("errors", errors.size() > 20 ? errors.subList(0, 20) : errors);
            if (errors.size() > 20) {
                result.put("totalErrors", errors.size());
            }
        }

        return result;
    }

    // ==================== CRUD de Tipos de Archivo Complementario ====================

    @Override
    public List<ComplementaryFileType> getComplementaryTypesBySubPortfolio(Integer subPortfolioId) {
        return complementaryFileTypeRepository.findBySubPortfolioId(subPortfolioId);
    }

    @Override
    public ComplementaryFileType getComplementaryTypeById(Integer id) {
        return complementaryFileTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de archivo complementario no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public ComplementaryFileType createComplementaryType(Integer subPortfolioId, String typeName,
                                                          String filePattern, String linkField,
                                                          List<String> columnsToUpdate, String description) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subcartera no encontrada con ID: " + subPortfolioId));

        // Validar que no exista otro tipo con el mismo nombre
        if (complementaryFileTypeRepository.existsBySubPortfolioAndTypeName(subPortfolio, typeName)) {
            throw new IllegalArgumentException(
                    "Ya existe un tipo de archivo complementario con el nombre: " + typeName);
        }

        // Validar el patrón regex
        try {
            Pattern.compile(filePattern);
        } catch (Exception e) {
            throw new IllegalArgumentException("Patrón de archivo inválido: " + e.getMessage());
        }

        // Convertir columnas a JSON
        String columnsJson;
        try {
            columnsJson = objectMapper.writeValueAsString(columnsToUpdate);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error al procesar columnas: " + e.getMessage());
        }

        ComplementaryFileType fileType = new ComplementaryFileType(
                subPortfolio, typeName, filePattern, linkField, columnsJson, description);

        ComplementaryFileType saved = complementaryFileTypeRepository.save(fileType);
        logger.info("✅ Tipo de archivo complementario creado: {} para subcartera {}",
                typeName, subPortfolio.getSubPortfolioName());

        return saved;
    }

    @Override
    @Transactional
    public ComplementaryFileType updateComplementaryType(Integer id, String typeName, String filePattern,
                                                          String linkField, List<String> columnsToUpdate,
                                                          String description, Boolean isActive) {
        ComplementaryFileType fileType = getComplementaryTypeById(id);

        if (typeName != null && !typeName.equals(fileType.getTypeName())) {
            // Validar que no exista otro con el nuevo nombre
            if (complementaryFileTypeRepository.existsBySubPortfolioAndTypeName(
                    fileType.getSubPortfolio(), typeName)) {
                throw new IllegalArgumentException(
                        "Ya existe un tipo de archivo complementario con el nombre: " + typeName);
            }
            fileType.setTypeName(typeName);
        }

        if (filePattern != null) {
            try {
                Pattern.compile(filePattern);
            } catch (Exception e) {
                throw new IllegalArgumentException("Patrón de archivo inválido: " + e.getMessage());
            }
            fileType.setFileNamePattern(filePattern);
        }

        if (linkField != null) {
            fileType.setLinkField(linkField);
        }

        if (columnsToUpdate != null) {
            try {
                fileType.setColumnsToUpdate(objectMapper.writeValueAsString(columnsToUpdate));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error al procesar columnas: " + e.getMessage());
            }
        }

        if (description != null) {
            fileType.setDescription(description);
        }

        if (isActive != null) {
            fileType.setIsActive(isActive ? 1 : 0);
        }

        return complementaryFileTypeRepository.save(fileType);
    }

    @Override
    @Transactional
    public void deleteComplementaryType(Integer id) {
        ComplementaryFileType fileType = getComplementaryTypeById(id);
        complementaryFileTypeRepository.delete(fileType);
        logger.info("Tipo de archivo complementario eliminado: {}", fileType.getTypeName());
    }

    @Override
    public ComplementaryFileType detectComplementaryType(Integer subPortfolioId, String fileName) {
        List<ComplementaryFileType> types = complementaryFileTypeRepository
                .findActiveBySubPortfolioId(subPortfolioId);

        for (ComplementaryFileType type : types) {
            try {
                Pattern pattern = Pattern.compile(type.getFileNamePattern(), Pattern.CASE_INSENSITIVE);
                // Usar find() en lugar de matches() para permitir coincidencias parciales
                // Esto permite patrones como "Facilidades_Pago_CONTACTO_TOTAL_\d{6}"
                // que coincidan con archivos como "Facilidades_Pago_CONTACTO_TOTAL_202501.xlsx"
                if (pattern.matcher(fileName).find()) {
                    logger.debug("Archivo '{}' coincide con tipo '{}'", fileName, type.getTypeName());
                    return type;
                }
            } catch (Exception e) {
                logger.warn("Error al evaluar patrón para tipo '{}': {}", type.getTypeName(), e.getMessage());
            }
        }

        return null; // No coincide con ningún tipo
    }

    @Override
    public List<String> validateColumnsExist(Integer subPortfolioId, List<String> columns) {
        List<HeaderConfiguration> headers = headerConfigurationRepository
                .findBySubPortfolioIdAndLoadType(subPortfolioId, LoadType.INICIAL);

        Set<String> existingColumns = headers.stream()
                .map(h -> h.getHeaderName().toLowerCase())
                .collect(Collectors.toSet());

        return columns.stream()
                .filter(col -> !existingColumns.contains(col.toLowerCase()))
                .collect(Collectors.toList());
    }

    // ==================== Métodos privados de utilidad ====================

    /**
     * Construye el nombre de la tabla dinámica
     */
    private String buildTableName(SubPortfolio subPortfolio, LoadType loadType) {
        String tenantCode = subPortfolio.getPortfolio().getTenant().getTenantCode().toLowerCase();
        String portfolioCode = subPortfolio.getPortfolio().getPortfolioCode().toLowerCase();
        String subPortfolioCode = subPortfolio.getSubPortfolioCode().toLowerCase();
        String baseName = tenantCode + "_" + portfolioCode + "_" + subPortfolioCode;
        return loadType.getTablePrefix() + baseName;
    }

    /**
     * Verifica si una tabla existe
     */
    private boolean dynamicTableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Error al verificar existencia de tabla {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica que las columnas existan en la tabla
     */
    private void verifyColumnsExistInTable(String tableName, String linkColumn, Collection<String> columns) {
        String sql = "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ?";

        List<String> existingColumns = jdbcTemplate.queryForList(sql, String.class, tableName);
        Set<String> existingSet = existingColumns.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Verificar columna de vinculación
        if (!existingSet.contains(linkColumn.toLowerCase())) {
            throw new IllegalArgumentException(
                    "La columna de vinculación '" + linkColumn + "' no existe en la tabla '" + tableName + "'");
        }

        // Verificar columnas a actualizar
        List<String> missing = columns.stream()
                .filter(col -> !existingSet.contains(col.toLowerCase()))
                .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Las siguientes columnas no existen en la tabla '" + tableName + "': " + missing);
        }
    }

    /**
     * Sanitiza el nombre de la columna
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
     * Obtiene un valor del rowData de forma case-insensitive
     */
    private Object getValueFromRowData(Map<String, Object> rowData, String headerName) {
        if (headerName == null) return null;

        // Búsqueda exacta
        if (rowData.containsKey(headerName)) {
            return rowData.get(headerName);
        }

        // Búsqueda case-insensitive
        String normalizedSearch = headerName.toLowerCase().trim();
        for (Map.Entry<String, Object> entry : rowData.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase().trim().equals(normalizedSearch)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Parsea el JSON de columnas a actualizar
     */
    private List<String> parseColumnsToUpdate(String columnsJson) {
        try {
            return objectMapper.readValue(columnsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error al parsear columnas: " + e.getMessage());
        }
    }
}
