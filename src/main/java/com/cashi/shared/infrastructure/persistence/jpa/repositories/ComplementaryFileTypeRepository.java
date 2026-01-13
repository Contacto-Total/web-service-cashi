package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.ComplementaryFileType;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestión de tipos de archivos complementarios
 */
@Repository
public interface ComplementaryFileTypeRepository extends JpaRepository<ComplementaryFileType, Integer> {

    /**
     * Obtiene todos los tipos de archivo de una subcartera
     */
    List<ComplementaryFileType> findBySubPortfolio(SubPortfolio subPortfolio);

    /**
     * Obtiene todos los tipos de archivo activos de una subcartera
     */
    @Query("SELECT cft FROM ComplementaryFileType cft " +
           "WHERE cft.subPortfolio = :subPortfolio AND cft.isActive = 1")
    List<ComplementaryFileType> findActiveBySubPortfolio(@Param("subPortfolio") SubPortfolio subPortfolio);

    /**
     * Obtiene tipos de archivo por ID de subcartera
     */
    List<ComplementaryFileType> findBySubPortfolioId(Integer subPortfolioId);

    /**
     * Obtiene tipos activos por ID de subcartera
     */
    @Query("SELECT cft FROM ComplementaryFileType cft " +
           "WHERE cft.subPortfolio.id = :subPortfolioId AND cft.isActive = 1")
    List<ComplementaryFileType> findActiveBySubPortfolioId(@Param("subPortfolioId") Integer subPortfolioId);

    /**
     * Busca un tipo de archivo por subcartera y nombre de tipo
     */
    Optional<ComplementaryFileType> findBySubPortfolioAndTypeName(SubPortfolio subPortfolio, String typeName);

    /**
     * Busca un tipo de archivo por ID de subcartera y nombre de tipo
     */
    Optional<ComplementaryFileType> findBySubPortfolioIdAndTypeName(Integer subPortfolioId, String typeName);

    /**
     * Verifica si existe un tipo de archivo con el mismo nombre en la subcartera
     */
    boolean existsBySubPortfolioAndTypeName(SubPortfolio subPortfolio, String typeName);

    /**
     * Elimina todos los tipos de archivo de una subcartera
     */
    void deleteBySubPortfolio(SubPortfolio subPortfolio);

    /**
     * Cuenta tipos de archivo de una subcartera
     */
    long countBySubPortfolio(SubPortfolio subPortfolio);
}
