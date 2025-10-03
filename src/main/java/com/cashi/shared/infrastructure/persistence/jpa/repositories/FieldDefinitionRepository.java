package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.FieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * FieldDefinitionRepository - Spring Data JPA repository for FieldDefinition entity
 */
@Repository
public interface FieldDefinitionRepository extends JpaRepository<FieldDefinition, Long> {

    /**
     * Find field definition by code
     */
    Optional<FieldDefinition> findByFieldCode(String fieldCode);

    /**
     * Find field definitions by category
     */
    List<FieldDefinition> findByFieldCategory(String fieldCategory);

    /**
     * Find field definitions by type
     */
    List<FieldDefinition> findByFieldType(FieldDefinition.FieldType fieldType);

    /**
     * Find all system fields
     */
    @Query("SELECT f FROM FieldDefinition f WHERE f.isSystemField = true ORDER BY f.displayOrder")
    List<FieldDefinition> findSystemFields();

    /**
     * Find all custom fields
     */
    @Query("SELECT f FROM FieldDefinition f WHERE f.isSystemField = false ORDER BY f.fieldCategory, f.displayOrder")
    List<FieldDefinition> findCustomFields();

    /**
     * Find fields by category ordered by display order
     */
    @Query("SELECT f FROM FieldDefinition f WHERE f.fieldCategory = :category ORDER BY f.displayOrder")
    List<FieldDefinition> findByCategoryOrderedByDisplayOrder(@Param("category") String category);

    /**
     * Check if field code exists
     */
    boolean existsByFieldCode(String fieldCode);
}
