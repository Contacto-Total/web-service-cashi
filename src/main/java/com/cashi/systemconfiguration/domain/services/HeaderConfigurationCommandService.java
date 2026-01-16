package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.valueobjects.LoadType;

import java.util.List;

public interface HeaderConfigurationCommandService {

    /**
     * Crea una nueva configuración de cabecera
     * @param subPortfolioId ID de la subcartera
     * @param fieldDefinitionId ID del campo del catálogo (null para campos personalizados)
     * @param headerName Nombre de la cabecera
     * @param dataType Tipo de dato (requerido si fieldDefinitionId es null)
     * @param displayLabel Etiqueta visual
     * @param format Formato específico
     * @param required Si es obligatorio
     * @param loadType Tipo de carga
     * @param sourceField Campo origen para transformación (opcional)
     * @param regexPattern Patrón regex para transformación (opcional)
     */
    HeaderConfiguration createHeaderConfiguration(Integer subPortfolioId, Integer fieldDefinitionId,
                                                  String headerName, String dataType, String displayLabel,
                                                  String format, Boolean required, LoadType loadType,
                                                  String sourceField, String regexPattern);

    /**
     * Actualiza una configuración de cabecera existente
     */
    HeaderConfiguration updateHeaderConfiguration(Integer id, String displayLabel,
                                                  String format, Boolean required, LoadType loadType);

    /**
     * Elimina una configuración de cabecera
     */
    void deleteHeaderConfiguration(Integer id);

    /**
     * Crea múltiples configuraciones de cabecera en lote (para importar CSV)
     */
    List<HeaderConfiguration> createBulkHeaderConfigurations(Integer subPortfolioId,
                                                             LoadType loadType,
                                                             List<HeaderConfigurationData> headers);

    /**
     * Elimina todas las configuraciones de una subcartera y tipo de carga específicos
     */
    void deleteAllBySubPortfolioAndLoadType(Integer subPortfolioId, LoadType loadType);

    /**
     * Importa datos masivos a la tabla dinámica de una subcartera
     */
    java.util.Map<String, Object> importDataToTable(Integer subPortfolioId, LoadType loadType, List<java.util.Map<String, Object>> data);

    /**
     * Actualiza datos complementarios en la tabla dinámica existente.
     * Busca registros por el campo de enlace (linkField) y actualiza las columnas proporcionadas.
     * Usado para archivos como PKM y Facilidades que actualizan columnas específicas.
     *
     * @param subPortfolioId ID de la subcartera
     * @param loadType Tipo de carga (INICIAL para archivos complementarios)
     * @param data Lista de registros con columnas a actualizar
     * @param linkField Campo usado para identificar el registro (ej: COD_CLI)
     * @return Mapa con estadísticas de la operación
     */
    java.util.Map<String, Object> updateComplementaryDataInTable(Integer subPortfolioId, LoadType loadType,
                                                                  List<java.util.Map<String, Object>> data, String linkField);

    /**
     * Importa datos de carga diaria.
     * Esta operación:
     * 1. Inserta/Actualiza datos en la tabla ACTUALIZACION (histórico diario)
     * 2. Actualiza los registros correspondientes en la tabla INICIAL (tabla maestra)
     * 3. Sincroniza los clientes SOLO desde la tabla INICIAL
     *
     * @param subPortfolioId ID de la subcartera
     * @param data Lista de registros a importar
     * @return Mapa con estadísticas de la operación
     */
    java.util.Map<String, Object> importDailyData(Integer subPortfolioId, List<java.util.Map<String, Object>> data);

    record HeaderConfigurationData(
        Integer fieldDefinitionId,
        String headerName,
        String dataType,
        String displayLabel,
        String format,
        Boolean required,
        String sourceField,
        String regexPattern
    ) {}
}
