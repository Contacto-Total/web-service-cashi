package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.systemconfiguration.domain.model.entities.ManagementClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @deprecated Use normalized ClassificationCatalog instead
 */
@Deprecated(forRemoval = true)
@Repository("legacyManagementClassificationRepository")
public interface LegacyManagementClassificationRepository extends JpaRepository<ManagementClassification, Long> {
    Optional<ManagementClassification> findByCode(String code);
    List<ManagementClassification> findByRequiresPayment(Boolean requiresPayment);
    List<ManagementClassification> findByRequiresSchedule(Boolean requiresSchedule);
    boolean existsByCode(String code);
}
