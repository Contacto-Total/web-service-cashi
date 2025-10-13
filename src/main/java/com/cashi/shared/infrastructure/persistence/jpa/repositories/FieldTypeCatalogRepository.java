package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.FieldTypeCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldTypeCatalogRepository extends JpaRepository<FieldTypeCatalog, Long> {

    /**
     * Busca un tipo por su código
     */
    Optional<FieldTypeCatalog> findByTypeCode(String typeCode);

    /**
     * Obtiene todos los tipos activos ordenados por displayOrder
     */
    @Query("SELECT ft FROM FieldTypeCatalog ft WHERE ft.isActive = true ORDER BY ft.displayOrder ASC")
    List<FieldTypeCatalog> findAllActiveOrderedByDisplay();

    /**
     * Obtiene tipos disponibles para campos principales
     */
    @Query("SELECT ft FROM FieldTypeCatalog ft WHERE ft.isActive = true AND ft.availableForMainField = true ORDER BY ft.displayOrder ASC")
    List<FieldTypeCatalog> findAvailableForMainField();

    /**
     * Obtiene tipos disponibles para columnas de tabla
     */
    @Query("SELECT ft FROM FieldTypeCatalog ft WHERE ft.isActive = true AND ft.availableForTableColumn = true ORDER BY ft.displayOrder ASC")
    List<FieldTypeCatalog> findAvailableForTableColumn();
}
