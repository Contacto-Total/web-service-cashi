package com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories;

import com.cashi.paymentprocessing.domain.model.entities.InstallmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstallmentStatusHistoryRepository extends JpaRepository<InstallmentStatusHistory, Long> {

    /**
     * Obtiene todo el historial de estados para una cuota específica
     */
    List<InstallmentStatusHistory> findByInstallmentIdOrderByChangeDateDesc(Long installmentId);

    /**
     * Obtiene el estado más reciente de una cuota
     */
    @Query("SELECT h FROM InstallmentStatusHistory h WHERE h.installmentId = :installmentId " +
           "ORDER BY h.changeDate DESC LIMIT 1")
    Optional<InstallmentStatusHistory> findLatestByInstallmentId(@Param("installmentId") Long installmentId);

    /**
     * Obtiene todos los historiales de estados para una gestión
     */
    List<InstallmentStatusHistory> findByManagementIdOrderByChangeDateDesc(String managementId);

    /**
     * Obtiene el último estado de cada cuota de una gestión
     */
    @Query("SELECT h FROM InstallmentStatusHistory h WHERE h.managementId = :managementId " +
           "AND h.changeDate = (SELECT MAX(h2.changeDate) FROM InstallmentStatusHistory h2 " +
           "WHERE h2.installmentId = h.installmentId)")
    List<InstallmentStatusHistory> findLatestStatusByManagementId(@Param("managementId") String managementId);
}
