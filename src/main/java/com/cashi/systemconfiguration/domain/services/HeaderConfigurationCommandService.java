package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.valueobjects.LoadType;

import java.util.List;

public interface HeaderConfigurationCommandService {

    /**
     * Crea una nueva configuración de cabecera
     */
    HeaderConfiguration createHeaderConfiguration(Integer subPortfolioId, Integer fieldDefinitionId,
                                                  String headerName, String displayLabel,
                                                  String format, Boolean required, LoadType loadType);

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

    record HeaderConfigurationData(
        Integer fieldDefinitionId,
        String headerName,
        String dataType,
        String displayLabel,
        String format,
        Boolean required
    ) {}
}
