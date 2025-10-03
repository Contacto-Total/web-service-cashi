package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.systemconfiguration.domain.model.entities.ContactClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @deprecated Use ClassificationCatalog repositories instead
 */
@Deprecated(forRemoval = true)
@Repository("legacyContactClassificationRepository")
public interface LegacyContactClassificationRepository extends JpaRepository<ContactClassification, Long> {
    Optional<ContactClassification> findByCode(String code);
    boolean existsByCode(String code);
}
