package com.cashi.systemconfiguration.application.internal.commandservices;

import com.cashi.shared.domain.model.entities.*;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.*;
import com.cashi.systemconfiguration.domain.services.HeaderResolutionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de resolución de cabeceras
 * Maneja el sistema de alias y auto-detección de columnas
 */
@Service
public class HeaderResolutionServiceImpl implements HeaderResolutionService {

    private static final Logger logger = LoggerFactory.getLogger(HeaderResolutionServiceImpl.class);

    private final HeaderConfigurationRepository headerConfigurationRepository;
    private final HeaderAliasRepository headerAliasRepository;
    private final HeaderChangeHistoryRepository changeHistoryRepository;
    private final SubPortfolioRepository subPortfolioRepository;
    private final ObjectMapper objectMapper;

    public HeaderResolutionServiceImpl(
            HeaderConfigurationRepository headerConfigurationRepository,
            HeaderAliasRepository headerAliasRepository,
            HeaderChangeHistoryRepository changeHistoryRepository,
            SubPortfolioRepository subPortfolioRepository,
            ObjectMapper objectMapper) {
        this.headerConfigurationRepository = headerConfigurationRepository;
        this.headerAliasRepository = headerAliasRepository;
        this.changeHistoryRepository = changeHistoryRepository;
        this.subPortfolioRepository = subPortfolioRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public HeaderResolutionResult resolveHeaders(Integer subPortfolioId, LoadType loadType, List<String> excelHeaders) {
        logger.info("Resolviendo cabeceras para SubPortfolio={}, LoadType={}, Headers={}",
                    subPortfolioId, loadType, excelHeaders.size());

        // 1. Obtener configuraciones de cabeceras con alias
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada: " + subPortfolioId));

        List<HeaderConfiguration> configurations = headerConfigurationRepository
                .findBySubPortfolioAndLoadType(subPortfolio, loadType);

        // 2. Obtener columnas ignoradas
        Set<String> ignoredColumnsSet = getIgnoredColumns(subPortfolioId, loadType);

        // 3. Construir mapa de alias -> cabecera interna
        Map<String, HeaderConfiguration> aliasToHeaderMap = buildAliasMap(configurations);

        // 4. Resolver cada cabecera del Excel
        Map<String, String> resolvedMapping = new LinkedHashMap<>();
        List<String> unrecognizedColumns = new ArrayList<>();
        List<String> ignoredColumns = new ArrayList<>();

        for (String excelHeader : excelHeaders) {
            String normalizedExcel = normalizeHeaderName(excelHeader);

            // Verificar si está en la lista de ignoradas
            if (isInIgnoredList(normalizedExcel, ignoredColumnsSet)) {
                ignoredColumns.add(excelHeader);
                logger.debug("Columna ignorada: {}", excelHeader);
                continue;
            }

            // Buscar match por alias o nombre directo
            HeaderConfiguration matchedConfig = findMatchingConfiguration(normalizedExcel, aliasToHeaderMap);

            if (matchedConfig != null) {
                resolvedMapping.put(excelHeader, matchedConfig.getHeaderName());
                logger.debug("Columna resuelta: {} -> {}", excelHeader, matchedConfig.getHeaderName());
            } else {
                unrecognizedColumns.add(excelHeader);
                logger.debug("Columna no reconocida: {}", excelHeader);
            }
        }

        // 5. Identificar cabeceras obligatorias faltantes
        List<String> missingRequired = findMissingRequiredHeaders(configurations, resolvedMapping.values());

        // 6. Construir lista de cabeceras con alias para respuesta
        List<HeaderConfigurationWithAliases> configuredHeaders = configurations.stream()
                .map(this::toHeaderWithAliases)
                .collect(Collectors.toList());

        logger.info("Resolución completada: {} resueltas, {} no reconocidas, {} ignoradas, {} requeridas faltantes",
                    resolvedMapping.size(), unrecognizedColumns.size(), ignoredColumns.size(), missingRequired.size());

        return new HeaderResolutionResult(
                resolvedMapping,
                unrecognizedColumns,
                ignoredColumns,
                configuredHeaders,
                missingRequired
        );
    }

    @Override
    @Transactional
    public HeaderAlias addAlias(Integer headerConfigId, String alias, String username) {
        HeaderConfiguration config = headerConfigurationRepository.findById(headerConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada: " + headerConfigId));

        // Verificar que el alias no exista ya en la misma subcartera/tipo de carga
        Optional<HeaderAlias> existing = headerAliasRepository.findBySubPortfolioAndLoadTypeAndAliasIgnoreCase(
                config.getSubPortfolio().getId(), config.getLoadType(), alias);

        if (existing.isPresent()) {
            throw new IllegalArgumentException("El alias '" + alias + "' ya existe para otra cabecera");
        }

        // Crear el alias
        HeaderAlias newAlias = new HeaderAlias(config, alias, false);
        headerAliasRepository.save(newAlias);

        // Registrar en historial
        HeaderChangeHistory history = HeaderChangeHistory.aliasAdded(
                config.getSubPortfolio().getId(),
                config.getLoadType(),
                alias,
                config.getHeaderName(),
                headerConfigId,
                username
        );
        changeHistoryRepository.save(history);

        logger.info("Alias '{}' agregado a cabecera '{}' por usuario '{}'",
                    alias, config.getHeaderName(), username);

        return newAlias;
    }

    @Override
    @Transactional
    public void removeAlias(Integer aliasId, String username) {
        HeaderAlias alias = headerAliasRepository.findById(aliasId)
                .orElseThrow(() -> new IllegalArgumentException("Alias no encontrado: " + aliasId));

        // No permitir eliminar el alias principal
        if (alias.isPrincipal()) {
            throw new IllegalArgumentException("No se puede eliminar el alias principal de una cabecera");
        }

        HeaderConfiguration config = alias.getHeaderConfiguration();

        // Registrar en historial antes de eliminar
        HeaderChangeHistory history = new HeaderChangeHistory(
                config.getSubPortfolio().getId(),
                config.getLoadType(),
                HeaderChangeHistory.ChangeType.ALIAS_REMOVIDO,
                alias.getAlias(),
                config.getHeaderName(),
                config.getId(),
                username
        );
        changeHistoryRepository.save(history);

        headerAliasRepository.delete(alias);

        logger.info("Alias '{}' eliminado de cabecera '{}' por usuario '{}'",
                    alias.getAlias(), config.getHeaderName(), username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeaderAlias> getAliasesByHeaderConfig(Integer headerConfigId) {
        return headerAliasRepository.findByHeaderConfigurationId(headerConfigId);
    }

    @Override
    @Transactional
    public HeaderConfiguration createNewHeader(Integer subPortfolioId, LoadType loadType,
                                                NewHeaderData data, String username) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada: " + subPortfolioId));

        // Verificar que no exista una cabecera con el mismo nombre
        boolean exists = headerConfigurationRepository.existsBySubPortfolioAndHeaderNameAndLoadType(
                subPortfolio, data.headerName(), loadType);

        if (exists) {
            throw new IllegalArgumentException("Ya existe una cabecera con el nombre: " + data.headerName());
        }

        // Crear la configuración
        HeaderConfiguration config = new HeaderConfiguration(
                subPortfolio,
                data.headerName(),
                data.dataType(),
                data.displayLabel(),
                data.format(),
                data.required() ? 1 : 0,
                loadType
        );
        headerConfigurationRepository.save(config);

        // Crear el alias principal
        HeaderAlias principalAlias = new HeaderAlias(config, data.headerName(), true);
        headerAliasRepository.save(principalAlias);

        // Registrar en historial
        HeaderChangeHistory history = HeaderChangeHistory.newColumn(
                subPortfolioId, loadType, data.headerName(), username);
        changeHistoryRepository.save(history);

        logger.info("Nueva cabecera '{}' creada en SubPortfolio={} por usuario '{}'",
                    data.headerName(), subPortfolioId, username);

        return config;
    }

    @Override
    @Transactional
    public void ignoreColumn(Integer subPortfolioId, LoadType loadType, String columnName, String username) {
        // Obtener columnas ignoradas actuales
        Set<String> ignoredSet = getIgnoredColumns(subPortfolioId, loadType);
        ignoredSet.add(columnName.toLowerCase());

        // Guardar en la primera configuración de la subcartera (o crear una entrada especial)
        saveIgnoredColumns(subPortfolioId, loadType, ignoredSet);

        // Registrar en historial
        HeaderChangeHistory history = HeaderChangeHistory.ignoredColumn(
                subPortfolioId, loadType, columnName, username);
        changeHistoryRepository.save(history);

        logger.info("Columna '{}' marcada como ignorada en SubPortfolio={} por usuario '{}'",
                    columnName, subPortfolioId, username);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getIgnoredColumns(Integer subPortfolioId, LoadType loadType) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId).orElse(null);
        if (subPortfolio == null) {
            return new HashSet<>();
        }

        List<HeaderConfiguration> configs = headerConfigurationRepository
                .findBySubPortfolioAndLoadType(subPortfolio, loadType);

        // Buscar en cualquier configuración que tenga columnas ignoradas
        for (HeaderConfiguration config : configs) {
            if (config.getIgnoredColumns() != null && !config.getIgnoredColumns().isEmpty()) {
                try {
                    List<String> ignored = objectMapper.readValue(
                            config.getIgnoredColumns(), new TypeReference<List<String>>() {});
                    return new HashSet<>(ignored.stream()
                            .map(String::toLowerCase)
                            .collect(Collectors.toList()));
                } catch (JsonProcessingException e) {
                    logger.warn("Error al parsear columnas ignoradas: {}", e.getMessage());
                }
            }
        }

        return new HashSet<>();
    }

    @Override
    @Transactional
    public void unignoreColumn(Integer subPortfolioId, LoadType loadType, String columnName, String username) {
        Set<String> ignoredSet = getIgnoredColumns(subPortfolioId, loadType);
        ignoredSet.remove(columnName.toLowerCase());
        saveIgnoredColumns(subPortfolioId, loadType, ignoredSet);

        logger.info("Columna '{}' removida de ignoradas en SubPortfolio={} por usuario '{}'",
                    columnName, subPortfolioId, username);
    }

    @Override
    @Transactional
    public HeaderAlias assignColumnAsAlias(Integer headerConfigId, String excelColumnName, String username) {
        // Simplemente reutilizamos addAlias
        return addAlias(headerConfigId, excelColumnName, username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeaderChangeHistory> getChangeHistory(Integer subPortfolioId, int limit) {
        return changeHistoryRepository.findRecentBySubPortfolioId(subPortfolioId, limit);
    }

    // ========== MÉTODOS PRIVADOS ==========

    /**
     * Construye un mapa de alias normalizados a configuraciones
     */
    private Map<String, HeaderConfiguration> buildAliasMap(List<HeaderConfiguration> configurations) {
        Map<String, HeaderConfiguration> map = new HashMap<>();

        for (HeaderConfiguration config : configurations) {
            // Agregar nombre principal
            map.put(normalizeHeaderName(config.getHeaderName()), config);

            // Agregar alias
            for (HeaderAlias alias : config.getAliases()) {
                map.put(normalizeHeaderName(alias.getAlias()), config);
            }
        }

        return map;
    }

    /**
     * Busca una configuración que coincida con el nombre normalizado
     */
    private HeaderConfiguration findMatchingConfiguration(String normalizedName,
                                                           Map<String, HeaderConfiguration> aliasMap) {
        return aliasMap.get(normalizedName);
    }

    /**
     * Verifica si una columna está en la lista de ignoradas
     */
    private boolean isInIgnoredList(String normalizedName, Set<String> ignoredSet) {
        return ignoredSet.contains(normalizedName);
    }

    /**
     * Encuentra cabeceras obligatorias que no tienen match en el Excel
     */
    private List<String> findMissingRequiredHeaders(List<HeaderConfiguration> configurations,
                                                     Collection<String> matchedHeaders) {
        Set<String> matched = new HashSet<>(matchedHeaders);

        return configurations.stream()
                .filter(c -> c.getRequired() != null && c.getRequired() == 1)
                .filter(c -> !matched.contains(c.getHeaderName()))
                .map(HeaderConfiguration::getHeaderName)
                .collect(Collectors.toList());
    }

    /**
     * Convierte configuración a DTO con alias
     */
    private HeaderConfigurationWithAliases toHeaderWithAliases(HeaderConfiguration config) {
        List<String> aliasNames = config.getAliases().stream()
                .map(HeaderAlias::getAlias)
                .collect(Collectors.toList());

        return new HeaderConfigurationWithAliases(
                config.getId(),
                config.getHeaderName(),
                config.getDataType(),
                config.getDisplayLabel(),
                config.getRequired() != null && config.getRequired() == 1,
                aliasNames
        );
    }

    /**
     * Guarda las columnas ignoradas en JSON
     */
    private void saveIgnoredColumns(Integer subPortfolioId, LoadType loadType, Set<String> ignoredSet) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId).orElse(null);
        if (subPortfolio == null) return;

        List<HeaderConfiguration> configs = headerConfigurationRepository
                .findBySubPortfolioAndLoadType(subPortfolio, loadType);

        if (!configs.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(new ArrayList<>(ignoredSet));
                // Guardar en la primera configuración
                HeaderConfiguration first = configs.get(0);
                first.setIgnoredColumns(json);
                headerConfigurationRepository.save(first);
            } catch (JsonProcessingException e) {
                logger.error("Error al serializar columnas ignoradas: {}", e.getMessage());
            }
        }
    }

    /**
     * Normaliza el nombre de una cabecera para comparación
     * - Convierte a minúsculas
     * - Reemplaza espacios, guiones bajos y guiones por _
     * - Elimina caracteres especiales
     */
    private String normalizeHeaderName(String headerName) {
        if (headerName == null) return "";
        return headerName.toLowerCase()
                .replaceAll("[\\s_-]+", "_")
                .replaceAll("[^a-z0-9_áéíóúñ]", "")
                .replaceAll("á", "a")
                .replaceAll("é", "e")
                .replaceAll("í", "i")
                .replaceAll("ó", "o")
                .replaceAll("ú", "u")
                .replaceAll("ñ", "n");
    }
}
