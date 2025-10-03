package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.systemconfiguration.domain.model.entities.ClassificationConfigHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClassificationConfigHistoryRepository extends JpaRepository<ClassificationConfigHistory, Long> {

    List<ClassificationConfigHistory> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        ClassificationConfigHistory.EntityType entityType, Long entityId
    );

    Page<ClassificationConfigHistory> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        ClassificationConfigHistory.EntityType entityType, Long entityId, Pageable pageable
    );

    @Query("SELECT h FROM ClassificationConfigHistory h " +
           "WHERE h.tenant.id = :tenantId " +
           "ORDER BY h.createdAt DESC")
    Page<ClassificationConfigHistory> findByTenantIdOrderByCreatedAtDesc(
        @Param("tenantId") Long tenantId, Pageable pageable
    );

    @Query("SELECT h FROM ClassificationConfigHistory h " +
           "WHERE h.tenant.id = :tenantId AND h.portfolio.id = :portfolioId " +
           "ORDER BY h.createdAt DESC")
    Page<ClassificationConfigHistory> findByTenantIdAndPortfolioIdOrderByCreatedAtDesc(
        @Param("tenantId") Long tenantId, @Param("portfolioId") Long portfolioId, Pageable pageable
    );

    @Query("SELECT h FROM ClassificationConfigHistory h " +
           "WHERE h.changedBy = :userId " +
           "ORDER BY h.createdAt DESC")
    Page<ClassificationConfigHistory> findByChangedByOrderByCreatedAtDesc(
        @Param("userId") String userId, Pageable pageable
    );

    @Query("SELECT h FROM ClassificationConfigHistory h " +
           "WHERE h.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY h.createdAt DESC")
    List<ClassificationConfigHistory> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate
    );
}
