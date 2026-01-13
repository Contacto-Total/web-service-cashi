package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.HeaderChangeHistory;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para historial de cambios en configuración de cabeceras
 */
@Repository
public interface HeaderChangeHistoryRepository extends JpaRepository<HeaderChangeHistory, Integer> {

    /**
     * Obtiene historial de una subcartera ordenado por fecha descendente
     */
    List<HeaderChangeHistory> findBySubPortfolioIdOrderByChangedAtDesc(Integer subPortfolioId);

    /**
     * Obtiene historial de una subcartera y tipo de carga
     */
    List<HeaderChangeHistory> findBySubPortfolioIdAndLoadTypeOrderByChangedAtDesc(
            Integer subPortfolioId, LoadType loadType);

    /**
     * Obtiene historial por tipo de cambio
     */
    List<HeaderChangeHistory> findBySubPortfolioIdAndChangeTypeOrderByChangedAtDesc(
            Integer subPortfolioId, HeaderChangeHistory.ChangeType changeType);

    /**
     * Obtiene historial paginado de una subcartera
     */
    Page<HeaderChangeHistory> findBySubPortfolioIdOrderByChangedAtDesc(
            Integer subPortfolioId, Pageable pageable);

    /**
     * Obtiene historial de un rango de fechas
     */
    @Query("SELECT hch FROM HeaderChangeHistory hch " +
           "WHERE hch.subPortfolioId = :subPortfolioId " +
           "AND hch.changedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY hch.changedAt DESC")
    List<HeaderChangeHistory> findBySubPortfolioIdAndDateRange(
            @Param("subPortfolioId") Integer subPortfolioId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Cuenta cambios por tipo en una subcartera
     */
    long countBySubPortfolioIdAndChangeType(Integer subPortfolioId, HeaderChangeHistory.ChangeType changeType);

    /**
     * Obtiene los últimos N cambios de una subcartera
     */
    @Query("SELECT hch FROM HeaderChangeHistory hch " +
           "WHERE hch.subPortfolioId = :subPortfolioId " +
           "ORDER BY hch.changedAt DESC " +
           "LIMIT :limit")
    List<HeaderChangeHistory> findRecentBySubPortfolioId(
            @Param("subPortfolioId") Integer subPortfolioId,
            @Param("limit") int limit);

    /**
     * Verifica si una columna ya fue marcada como ignorada
     */
    @Query("SELECT COUNT(hch) > 0 FROM HeaderChangeHistory hch " +
           "WHERE hch.subPortfolioId = :subPortfolioId " +
           "AND hch.loadType = :loadType " +
           "AND hch.changeType = 'COLUMNA_IGNORADA' " +
           "AND LOWER(hch.excelColumnName) = LOWER(:columnName)")
    boolean isColumnIgnored(
            @Param("subPortfolioId") Integer subPortfolioId,
            @Param("loadType") LoadType loadType,
            @Param("columnName") String columnName);

    /**
     * Elimina historial de una subcartera
     */
    void deleteBySubPortfolioId(Integer subPortfolioId);
}
