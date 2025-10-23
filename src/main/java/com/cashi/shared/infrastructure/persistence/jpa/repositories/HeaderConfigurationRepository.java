package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para configuración de cabeceras personalizadas
 */
@Repository
public interface HeaderConfigurationRepository extends JpaRepository<HeaderConfiguration, Integer> {

    /**
     * Obtiene todas las configuraciones de cabeceras de una subcartera
     */
    List<HeaderConfiguration> findBySubPortfolio(SubPortfolio subPortfolio);

    /**
     * Obtiene todas las configuraciones de cabeceras de una subcartera filtradas por tipo de carga
     */
    List<HeaderConfiguration> findBySubPortfolioAndLoadType(SubPortfolio subPortfolio, LoadType loadType);

    /**
     * Verifica si existe una configuración con el mismo nombre de cabecera para una subcartera
     */
    boolean existsBySubPortfolioAndHeaderName(SubPortfolio subPortfolio, String headerName);

    /**
     * Verifica si existe una configuración con el mismo nombre de cabecera para una subcartera y tipo de carga
     */
    boolean existsBySubPortfolioAndHeaderNameAndLoadType(SubPortfolio subPortfolio, String headerName, LoadType loadType);

    /**
     * Busca una configuración específica por subcartera y nombre de cabecera
     */
    Optional<HeaderConfiguration> findBySubPortfolioAndHeaderName(SubPortfolio subPortfolio, String headerName);

    /**
     * Busca una configuración específica por subcartera, nombre de cabecera y tipo de carga
     */
    Optional<HeaderConfiguration> findBySubPortfolioAndHeaderNameAndLoadType(SubPortfolio subPortfolio, String headerName, LoadType loadType);

    /**
     * Elimina todas las configuraciones de una subcartera
     */
    void deleteBySubPortfolio(SubPortfolio subPortfolio);

    /**
     * Elimina todas las configuraciones de una subcartera y tipo de carga específicos
     */
    void deleteBySubPortfolioAndLoadType(SubPortfolio subPortfolio, LoadType loadType);

    /**
     * Cuenta cuántas configuraciones tiene una subcartera
     */
    long countBySubPortfolio(SubPortfolio subPortfolio);
}
