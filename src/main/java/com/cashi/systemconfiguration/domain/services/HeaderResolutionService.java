package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.HeaderAlias;
import com.cashi.shared.domain.model.entities.HeaderChangeHistory;
import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.valueobjects.LoadType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servicio para resolución de cabeceras con sistema de alias
 * Permite mapear nombres de columnas del Excel a cabeceras configuradas
 */
public interface HeaderResolutionService {

    /**
     * Resuelve las cabeceras del Excel contra la configuración de la subcartera
     * Retorna un resultado con:
     * - Mapeo de columnas resueltas (Excel -> interno)
     * - Lista de columnas no reconocidas
     * - Lista de cabeceras configuradas
     *
     * @param subPortfolioId ID de la subcartera
     * @param loadType Tipo de carga (INICIAL o ACTUALIZACION)
     * @param excelHeaders Lista de nombres de columnas del Excel
     * @return Resultado de la resolución
     */
    HeaderResolutionResult resolveHeaders(Integer subPortfolioId, LoadType loadType, List<String> excelHeaders);

    /**
     * Agrega un alias a una cabecera existente
     *
     * @param headerConfigId ID de la configuración de cabecera
     * @param alias Nombre alternativo a agregar
     * @param username Usuario que realiza el cambio
     * @return El alias creado
     */
    HeaderAlias addAlias(Integer headerConfigId, String alias, String username);

    /**
     * Elimina un alias de una cabecera
     *
     * @param aliasId ID del alias a eliminar
     * @param username Usuario que realiza el cambio
     */
    void removeAlias(Integer aliasId, String username);

    /**
     * Obtiene todos los alias de una cabecera
     *
     * @param headerConfigId ID de la configuración de cabecera
     * @return Lista de alias
     */
    List<HeaderAlias> getAliasesByHeaderConfig(Integer headerConfigId);

    /**
     * Crea una nueva cabecera a partir de una columna no reconocida
     *
     * @param subPortfolioId ID de la subcartera
     * @param loadType Tipo de carga
     * @param data Datos de la nueva cabecera
     * @param username Usuario que realiza el cambio
     * @return La configuración creada
     */
    HeaderConfiguration createNewHeader(Integer subPortfolioId, LoadType loadType,
                                         NewHeaderData data, String username);

    /**
     * Marca una columna como ignorada
     *
     * @param subPortfolioId ID de la subcartera
     * @param loadType Tipo de carga
     * @param columnName Nombre de la columna a ignorar
     * @param username Usuario que realiza el cambio
     */
    void ignoreColumn(Integer subPortfolioId, LoadType loadType, String columnName, String username);

    /**
     * Obtiene las columnas ignoradas de una subcartera
     *
     * @param subPortfolioId ID de la subcartera
     * @param loadType Tipo de carga
     * @return Set de nombres de columnas ignoradas
     */
    Set<String> getIgnoredColumns(Integer subPortfolioId, LoadType loadType);

    /**
     * Quita una columna de la lista de ignoradas
     *
     * @param subPortfolioId ID de la subcartera
     * @param loadType Tipo de carga
     * @param columnName Nombre de la columna
     * @param username Usuario que realiza el cambio
     */
    void unignoreColumn(Integer subPortfolioId, LoadType loadType, String columnName, String username);

    /**
     * Asigna una columna del Excel como alias de una cabecera existente
     *
     * @param headerConfigId ID de la cabecera destino
     * @param excelColumnName Nombre de la columna en el Excel
     * @param username Usuario que realiza el cambio
     * @return El alias creado
     */
    HeaderAlias assignColumnAsAlias(Integer headerConfigId, String excelColumnName, String username);

    /**
     * Obtiene el historial de cambios de una subcartera
     *
     * @param subPortfolioId ID de la subcartera
     * @param limit Número máximo de registros
     * @return Lista de cambios
     */
    List<HeaderChangeHistory> getChangeHistory(Integer subPortfolioId, int limit);

    /**
     * Resultado de la resolución de cabeceras
     */
    record HeaderResolutionResult(
        /** Mapeo: nombre en Excel -> nombre interno de la cabecera */
        Map<String, String> resolvedMapping,
        /** Columnas del Excel que no fueron reconocidas */
        List<String> unrecognizedColumns,
        /** Columnas del Excel que están en la lista de ignoradas */
        List<String> ignoredColumns,
        /** Cabeceras configuradas con sus alias */
        List<HeaderConfigurationWithAliases> configuredHeaders,
        /** Cabeceras configuradas que no tienen match en el Excel */
        List<String> missingRequiredHeaders
    ) {}

    /**
     * Cabecera con su lista de alias
     */
    record HeaderConfigurationWithAliases(
        Integer id,
        String headerName,
        String dataType,
        String displayLabel,
        boolean required,
        List<String> aliases
    ) {}

    /**
     * Datos para crear una nueva cabecera
     */
    record NewHeaderData(
        String headerName,
        String dataType,      // TEXTO, NUMERICO, FECHA
        String displayLabel,
        String format,
        boolean required
    ) {}
}
