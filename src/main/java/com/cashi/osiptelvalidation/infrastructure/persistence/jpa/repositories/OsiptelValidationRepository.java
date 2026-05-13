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
import java.util.Optional;

@Repository
public interface OsiptelValidationRepository extends JpaRepository<OsiptelValidation, Long> {

    /**
     * Existe ya una validación activa para este teléfono?
     * Activa = PENDING o IN_PROGRESS (protegido también por UNIQUE KEY a nivel BD).
     */
    boolean existsByPhoneAndStatusIn(String phone, List<ValidationStatus> statuses);

    /**
     * Última validación conocida para un número.
     * Prioriza finalizadas (finished_at no null); si todas están en curso, devuelve la más reciente por id.
     */
    @Query("SELECT v FROM OsiptelValidation v WHERE v.phone = :phone " +
           "ORDER BY CASE WHEN v.finishedAt IS NULL THEN 1 ELSE 0 END, " +
           "         v.finishedAt DESC, v.id DESC")
    List<OsiptelValidation> findLatestByPhone(@Param("phone") String phone);

    default Optional<OsiptelValidation> findLatestOne(String phone) {
        List<OsiptelValidation> list = findLatestByPhone(phone);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Reclama N filas PENDING con SKIP LOCKED para que múltiples instancias
     * del dispatcher no se peleen por el mismo trabajo.
     *
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
     * Filas en IN_PROGRESS con started_at más viejo que el umbral.
     * Significan workers que murieron a mitad del trabajo.
     */
    @Query("SELECT v FROM OsiptelValidation v WHERE v.status = " +
           "com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus.IN_PROGRESS " +
           "AND v.startedAt < :cutoff")
    List<OsiptelValidation> findStuckInProgress(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Conteos por status, opcionalmente filtrado por subcartera.
     */
    @Query("SELECT v.status, COUNT(v) FROM OsiptelValidation v " +
           "WHERE (:subPortfolioId IS NULL OR v.sourceSubPortfolioId = :subPortfolioId) " +
           "  AND (:from IS NULL OR v.fecha_creacion >= :from) " +
           "  AND (:to IS NULL OR v.fecha_creacion <= :to) " +
           "GROUP BY v.status")
    List<Object[]> countByStatus(@Param("subPortfolioId") Long subPortfolioId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    /**
     * Conteo dni_match true/false dentro del rango.
     * dniMatchValue: 1 (true) o 0 (false).
     */
    @Query("SELECT v.dniMatch, COUNT(v) FROM OsiptelValidation v " +
           "WHERE v.status = com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus.OK " +
           "  AND (:subPortfolioId IS NULL OR v.sourceSubPortfolioId = :subPortfolioId) " +
           "  AND (:from IS NULL OR v.finishedAt >= :from) " +
           "  AND (:to IS NULL OR v.finishedAt <= :to) " +
           "GROUP BY v.dniMatch")
    List<Object[]> countDniMatch(@Param("subPortfolioId") Long subPortfolioId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    /**
     * Conteo por operador dentro del rango.
     */
    @Query("SELECT v.operator, COUNT(v) FROM OsiptelValidation v " +
           "WHERE v.status = com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus.OK " +
           "  AND v.operator IS NOT NULL " +
           "  AND (:subPortfolioId IS NULL OR v.sourceSubPortfolioId = :subPortfolioId) " +
           "  AND (:from IS NULL OR v.finishedAt >= :from) " +
           "  AND (:to IS NULL OR v.finishedAt <= :to) " +
           "GROUP BY v.operator")
    List<Object[]> countByOperator(@Param("subPortfolioId") Long subPortfolioId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);
}
