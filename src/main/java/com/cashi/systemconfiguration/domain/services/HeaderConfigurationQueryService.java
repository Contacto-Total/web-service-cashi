package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.valueobjects.LoadType;

import java.util.List;
import java.util.Optional;

public interface HeaderConfigurationQueryService {

    /**
     * Obtiene todas las configuraciones de cabeceras de una subcartera
     */
    List<HeaderConfiguration> getAllBySubPortfolio(Integer subPortfolioId);

    /**
     * Obtiene todas las configuraciones de cabeceras de una subcartera filtradas por tipo de carga
     */
    List<HeaderConfiguration> getAllBySubPortfolioAndLoadType(Integer subPortfolioId, LoadType loadType);

    /**
     * Obtiene una configuración específica por ID
     */
    Optional<HeaderConfiguration> getById(Integer id);

    /**
     * Cuenta cuántas configuraciones tiene una subcartera
     */
    long countBySubPortfolio(Integer subPortfolioId);
}
