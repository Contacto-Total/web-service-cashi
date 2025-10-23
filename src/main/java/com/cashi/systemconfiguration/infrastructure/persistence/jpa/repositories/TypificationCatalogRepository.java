package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.systemconfiguration.domain.model.entities.TypificationCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypificationCatalogRepository extends JpaRepository<TypificationCatalog, Integer> {

    Optional<TypificationCatalog> findByCode(String code);

    List<TypificationCatalog> findByClassificationType(TypificationCatalog.ClassificationType classificationType);

    @Query("SELECT t FROM TypificationCatalog t WHERE t.deletedAt IS NULL AND t.isActive = 1 ORDER BY t.displayOrder")
    List<TypificationCatalog> findAllActive();

    @Query("SELECT t FROM TypificationCatalog t WHERE t.classificationType = :type AND t.deletedAt IS NULL AND t.isActive = 1 ORDER BY t.hierarchyLevel, t.displayOrder")
    List<TypificationCatalog> findActiveByType(@Param("type") TypificationCatalog.ClassificationType type);

    @Query("SELECT t FROM TypificationCatalog t WHERE t.parentTypification IS NULL AND t.classificationType = :type AND t.deletedAt IS NULL AND t.isActive = 1 ORDER BY t.displayOrder")
    List<TypificationCatalog> findRootByType(@Param("type") TypificationCatalog.ClassificationType type);

    @Query("SELECT t FROM TypificationCatalog t WHERE t.parentTypification.id = :parentId AND t.deletedAt IS NULL AND t.isActive = 1 ORDER BY t.displayOrder")
    List<TypificationCatalog> findByParentId(@Param("parentId") Integer parentId);

    @Query("SELECT t FROM TypificationCatalog t WHERE t.hierarchyLevel = :level AND t.classificationType = :type AND t.deletedAt IS NULL AND t.isActive = 1 ORDER BY t.displayOrder")
    List<TypificationCatalog> findByLevelAndType(@Param("level") Integer level, @Param("type") TypificationCatalog.ClassificationType type);

    @Query("SELECT t FROM TypificationCatalog t WHERE t.hierarchyPath LIKE :pathPattern AND t.deletedAt IS NULL ORDER BY t.hierarchyLevel, t.displayOrder")
    List<TypificationCatalog> findByHierarchyPath(@Param("pathPattern") String pathPattern);

    boolean existsByCode(String code);

    @Query("SELECT COUNT(t) > 0 FROM TypificationCatalog t WHERE t.parentTypification.id = :parentId AND t.deletedAt IS NULL")
    boolean hasChildren(@Param("parentId") Integer parentId);
}
