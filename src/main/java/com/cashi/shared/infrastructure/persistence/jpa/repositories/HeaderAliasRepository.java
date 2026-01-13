package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.HeaderAlias;
import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestión de alias de cabeceras
 */
@Repository
public interface HeaderAliasRepository extends JpaRepository<HeaderAlias, Integer> {

    /**
     * Obtiene todos los alias de una configuración de cabecera
     */
    List<HeaderAlias> findByHeaderConfiguration(HeaderConfiguration headerConfiguration);

    /**
     * Obtiene todos los alias de una configuración por su ID
     */
    List<HeaderAlias> findByHeaderConfigurationId(Integer headerConfigurationId);

    /**
     * Busca un alias específico en una configuración
     */
    Optional<HeaderAlias> findByHeaderConfigurationAndAlias(HeaderConfiguration headerConfiguration, String alias);

    /**
     * Verifica si existe un alias específico para una configuración
     */
    boolean existsByHeaderConfigurationAndAlias(HeaderConfiguration headerConfiguration, String alias);

    /**
     * Busca un alias por nombre (case-insensitive) dentro de una subcartera y tipo de carga
     * Útil para validar que un alias no esté duplicado en la misma subcartera
     */
    @Query("SELECT ha FROM HeaderAlias ha " +
           "JOIN ha.headerConfiguration hc " +
           "WHERE hc.subPortfolio.id = :subPortfolioId " +
           "AND hc.loadType = :loadType " +
           "AND LOWER(ha.alias) = LOWER(:alias)")
    Optional<HeaderAlias> findBySubPortfolioAndLoadTypeAndAliasIgnoreCase(
            @Param("subPortfolioId") Integer subPortfolioId,
            @Param("loadType") com.cashi.shared.domain.model.valueobjects.LoadType loadType,
            @Param("alias") String alias);

    /**
     * Obtiene todos los alias de una subcartera y tipo de carga
     */
    @Query("SELECT ha FROM HeaderAlias ha " +
           "JOIN FETCH ha.headerConfiguration hc " +
           "WHERE hc.subPortfolio.id = :subPortfolioId " +
           "AND hc.loadType = :loadType")
    List<HeaderAlias> findAllBySubPortfolioAndLoadType(
            @Param("subPortfolioId") Integer subPortfolioId,
            @Param("loadType") com.cashi.shared.domain.model.valueobjects.LoadType loadType);

    /**
     * Elimina todos los alias de una configuración
     */
    void deleteByHeaderConfiguration(HeaderConfiguration headerConfiguration);

    /**
     * Cuenta cuántos alias tiene una configuración
     */
    long countByHeaderConfiguration(HeaderConfiguration headerConfiguration);

    /**
     * Obtiene el alias principal de una configuración
     */
    @Query("SELECT ha FROM HeaderAlias ha WHERE ha.headerConfiguration = :config AND ha.esPrincipal = 1")
    Optional<HeaderAlias> findPrincipalByHeaderConfiguration(@Param("config") HeaderConfiguration headerConfiguration);
}
