package com.cashi.shared.application.services;

import com.cashi.shared.domain.model.entities.FieldTypeCatalog;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.FieldTypeCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestionar el catálogo de tipos de campo disponibles
 */
@Service
@Transactional(readOnly = true)
public class FieldTypeService {

    private final FieldTypeCatalogRepository fieldTypeCatalogRepository;

    public FieldTypeService(FieldTypeCatalogRepository fieldTypeCatalogRepository) {
        this.fieldTypeCatalogRepository = fieldTypeCatalogRepository;
    }

    /**
     * Obtiene todos los tipos de campo activos ordenados por displayOrder
     */
    public List<FieldTypeCatalog> getAllActiveFieldTypes() {
        return fieldTypeCatalogRepository.findAllActiveOrderedByDisplay();
    }

    /**
     * Obtiene tipos de campo disponibles para campos principales
     */
    public List<FieldTypeCatalog> getFieldTypesForMainFields() {
        return fieldTypeCatalogRepository.findAvailableForMainField();
    }

    /**
     * Obtiene tipos de campo disponibles para columnas de tabla
     */
    public List<FieldTypeCatalog> getFieldTypesForTableColumns() {
        return fieldTypeCatalogRepository.findAvailableForTableColumn();
    }

    /**
     * Busca un tipo de campo por su código
     */
    public Optional<FieldTypeCatalog> findByTypeCode(String typeCode) {
        return fieldTypeCatalogRepository.findByTypeCode(typeCode);
    }
}
