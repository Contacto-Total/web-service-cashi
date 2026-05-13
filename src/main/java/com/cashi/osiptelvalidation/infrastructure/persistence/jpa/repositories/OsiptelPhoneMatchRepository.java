package com.cashi.osiptelvalidation.infrastructure.persistence.jpa.repositories;

import com.cashi.osiptelvalidation.domain.model.entities.OsiptelPhoneMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OsiptelPhoneMatchRepository extends JpaRepository<OsiptelPhoneMatch, Long> {

    /**
     * El match más reciente conocido para un teléfono.
     * Lo consume el endpoint GET /validations/{phone}.
     */
    Optional<OsiptelPhoneMatch> findTopByPhoneOrderByCreatedAtDesc(String phone);

    List<OsiptelPhoneMatch> findByValidationId(Long validationId);

    /**
     * Conteos para reportería: total, matches, no-matches en el rango.
     */
    @Query("SELECT m.dniMatch, COUNT(m) FROM OsiptelPhoneMatch m " +
           "WHERE (:from IS NULL OR m.createdAt >= :from) " +
           "  AND (:to IS NULL OR m.createdAt <= :to) " +
           "GROUP BY m.dniMatch")
    List<Object[]> countByMatch(@Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    /**
     * Conteo por operador para los teléfonos que matchearon.
     */
    @Query("SELECT m.matchedOperator, COUNT(m) FROM OsiptelPhoneMatch m " +
           "WHERE m.dniMatch = true " +
           "  AND (:from IS NULL OR m.createdAt >= :from) " +
           "  AND (:to IS NULL OR m.createdAt <= :to) " +
           "GROUP BY m.matchedOperator")
    List<Object[]> countByOperator(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);
}
