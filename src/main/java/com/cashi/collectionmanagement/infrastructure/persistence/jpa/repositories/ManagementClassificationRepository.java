package com.cashi.collectionmanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.collectionmanagement.domain.model.entities.ManagementClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManagementClassificationRepository extends JpaRepository<ManagementClassification, Long> {

    List<ManagementClassification> findByManagementOrderByHierarchyLevel(Management management);

    Optional<ManagementClassification> findByManagementAndHierarchyLevel(Management management, Integer hierarchyLevel);

    @Query("SELECT mc FROM ManagementClassification mc " +
           "WHERE mc.management = :management " +
           "ORDER BY mc.hierarchyLevel ASC")
    List<ManagementClassification> findByManagementOrderedByLevel(@Param("management") Management management);

    @Query("SELECT mc FROM ManagementClassification mc " +
           "WHERE mc.management.id = :managementId AND mc.hierarchyLevel = :level")
    Optional<ManagementClassification> findByManagementIdAndLevel(
        @Param("managementId") Long managementId, @Param("level") Integer level
    );

    void deleteByManagement(Management management);

    @Query("SELECT COUNT(mc) FROM ManagementClassification mc WHERE mc.management = :management")
    long countByManagement(@Param("management") Management management);
}
