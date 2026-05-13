package com.cashi.osiptelvalidation.infrastructure.persistence.jpa.repositories;

import com.cashi.osiptelvalidation.domain.model.aggregates.OsiptelValidation;
import com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OsiptelValidationRepository extends JpaRepository<OsiptelValidation, Long> {

    /**
     * Existe ya una validación activa para este documento?
     * Activa = PENDING o IN_PROGRESS (también protegido por UNIQUE KEY a nivel BD).
     */
    boolean existsByDniHashAndStatusIn(String dniHash, List<ValidationStatus> statuses);

    /**
     * Reclama N filas PENDING con SKIP LOCKED para que múltiples instancias
     * del dispatcher no se peleen por el mismo trabajo.
     * MySQL 8.0.1+ soporta SKIP LOCKED en SELECT FOR UPDATE.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")})
    @Query(value = "SELECT * FROM osiptel_validation_log " +
                   "WHERE status = 'PENDING' " +
                   "  AND (cooldown_until IS NULL OR cooldown_until <= NOW()) " +
                   "ORDER BY enqueued_at ASC " +
                   "LIMIT :limit FOR UPDATE SKIP LOCKED",
           nativeQuery = true)
    List<OsiptelValidation> claimPendingForUpdate(@Param("limit") int limit);

    /**
     * Filas en IN_PROGRESS huérfanas (worker que murió).
     */
    @Query("SELECT v FROM OsiptelValidation v WHERE v.status = " +
           "com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus.IN_PROGRESS " +
           "AND v.startedAt < :cutoff")
    List<OsiptelValidation> findStuckInProgress(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Conteos por status para métricas.
     */
    @Query("SELECT v.status, COUNT(v) FROM OsiptelValidation v " +
           "WHERE (:subPortfolioId IS NULL OR v.sourceSubPortfolioId = :subPortfolioId) " +
           "  AND (:from IS NULL OR v.enqueuedAt >= :from) " +
           "  AND (:to IS NULL OR v.enqueuedAt <= :to) " +
           "GROUP BY v.status")
    List<Object[]> countByStatus(@Param("subPortfolioId") Long subPortfolioId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    /**
     * Total de líneas devueltas por validaciones OK en el rango.
     */
    @Query("SELECT COALESCE(SUM(v.linesCount), 0) FROM OsiptelValidation v " +
           "WHERE v.status = com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus.OK " +
           "  AND (:subPortfolioId IS NULL OR v.sourceSubPortfolioId = :subPortfolioId) " +
           "  AND (:from IS NULL OR v.finishedAt >= :from) " +
           "  AND (:to IS NULL OR v.finishedAt <= :to)")
    Long totalLinesReturned(@Param("subPortfolioId") Long subPortfolioId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);
}
