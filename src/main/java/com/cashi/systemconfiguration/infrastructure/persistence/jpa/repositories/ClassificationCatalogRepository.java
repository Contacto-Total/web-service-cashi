package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.systemconfiguration.domain.model.entities.ClassificationCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassificationCatalogRepository extends JpaRepository<ClassificationCatalog, Long> {

    Optional<ClassificationCatalog> findByCode(String code);

    List<ClassificationCatalog> findByClassificationType(ClassificationCatalog.ClassificationType classificationType);

    @Query("SELECT c FROM ClassificationCatalog c WHERE c.deletedAt IS NULL AND c.isActive = true ORDER BY c.displayOrder")
    List<ClassificationCatalog> findAllActive();

    @Query("SELECT c FROM ClassificationCatalog c WHERE c.classificationType = :type AND c.deletedAt IS NULL AND c.isActive = true ORDER BY c.hierarchyLevel, c.displayOrder")
    List<ClassificationCatalog> findActiveByType(@Param("type") ClassificationCatalog.ClassificationType type);

    @Query("SELECT c FROM ClassificationCatalog c WHERE c.parentClassification IS NULL AND c.classificationType = :type AND c.deletedAt IS NULL AND c.isActive = true ORDER BY c.displayOrder")
    List<ClassificationCatalog> findRootByType(@Param("type") ClassificationCatalog.ClassificationType type);

    @Query("SELECT c FROM ClassificationCatalog c WHERE c.parentClassification.id = :parentId AND c.deletedAt IS NULL AND c.isActive = true ORDER BY c.displayOrder")
    List<ClassificationCatalog> findByParentId(@Param("parentId") Long parentId);

    @Query("SELECT c FROM ClassificationCatalog c WHERE c.hierarchyLevel = :level AND c.classificationType = :type AND c.deletedAt IS NULL AND c.isActive = true ORDER BY c.displayOrder")
    List<ClassificationCatalog> findByLevelAndType(@Param("level") Integer level, @Param("type") ClassificationCatalog.ClassificationType type);

    @Query("SELECT c FROM ClassificationCatalog c WHERE c.hierarchyPath LIKE :pathPattern AND c.deletedAt IS NULL ORDER BY c.hierarchyLevel, c.displayOrder")
    List<ClassificationCatalog> findByHierarchyPath(@Param("pathPattern") String pathPattern);

    boolean existsByCode(String code);

    @Query("SELECT COUNT(c) > 0 FROM ClassificationCatalog c WHERE c.parentClassification.id = :parentId AND c.deletedAt IS NULL")
    boolean hasChildren(@Param("parentId") Long parentId);
}
