package com.cashi.collectionmanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.collectionmanagement.domain.model.entities.ManagementDynamicField;
import com.cashi.shared.domain.model.entities.FieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ManagementDynamicFieldRepository - Repository for management dynamic fields
 */
@Repository
public interface ManagementDynamicFieldRepository extends JpaRepository<ManagementDynamicField, Long> {

    /**
     * Find by management and field definition
     */
    Optional<ManagementDynamicField> findByManagementAndFieldDefinition(
        Management management, FieldDefinition fieldDefinition
    );

    /**
     * Find all dynamic fields for a management
     */
    List<ManagementDynamicField> findByManagement(Management management);

    /**
     * Find all dynamic fields for a management ordered by field definition
     */
    @Query("SELECT mdf FROM ManagementDynamicField mdf " +
           "WHERE mdf.management = :management " +
           "ORDER BY mdf.fieldDefinition.displayOrder")
    List<ManagementDynamicField> findByManagementOrderedByDisplayOrder(@Param("management") Management management);

    /**
     * Find dynamic fields by field definition
     */
    List<ManagementDynamicField> findByFieldDefinition(FieldDefinition fieldDefinition);

    /**
     * Delete all dynamic fields for a management
     */
    void deleteByManagement(Management management);
}
