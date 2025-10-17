package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.systemconfiguration.domain.model.entities.ClassificationTypeCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassificationTypeCatalogRepository extends JpaRepository<ClassificationTypeCatalog, Long> {

    Optional<ClassificationTypeCatalog> findByCategoryAndCode(String category, String code);

    List<ClassificationTypeCatalog> findByCategory(String category);

    List<ClassificationTypeCatalog> findByIsActiveTrue();

    List<ClassificationTypeCatalog> findByCategoryAndIsActiveTrue(String category);
}
