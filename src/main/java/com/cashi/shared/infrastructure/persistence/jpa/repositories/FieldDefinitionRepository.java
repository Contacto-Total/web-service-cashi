package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.FieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * FieldDefinitionRepository - Repositorio para el catálogo maestro de campos del sistema
 */
@Repository
public interface FieldDefinitionRepository extends JpaRepository<FieldDefinition, Integer> {

    /**
     * Buscar definición de campo por código
     */
    Optional<FieldDefinition> findByFieldCode(String fieldCode);

    /**
     * Buscar definiciones de campo por tipo de dato
     */
    List<FieldDefinition> findByDataType(String dataType);

    /**
     * Buscar todos los campos ordenados por ID (orden de inserción)
     */
    @Query("SELECT f FROM FieldDefinition f ORDER BY f.id")
    List<FieldDefinition> findAllOrderedByName();

    /**
     * Verificar si existe un campo con el código especificado
     */
    boolean existsByFieldCode(String fieldCode);
}
