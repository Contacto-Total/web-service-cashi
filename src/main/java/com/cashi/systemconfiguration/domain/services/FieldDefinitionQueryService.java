package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.FieldDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de consultas para el catálogo maestro de definiciones de campos
 */
public interface FieldDefinitionQueryService {

    /**
     * Obtiene todas las definiciones de campos activas
     */
    List<FieldDefinition> getAllActive();

    /**
     * Obtiene todas las definiciones de campos activas por categoría
     */
    List<FieldDefinition> getAllActiveByCategory(String category);

    /**
     * Obtiene todas las definiciones de campos activas por tipo de dato
     */
    List<FieldDefinition> getAllActiveByDataType(String dataType);

    /**
     * Obtiene una definición de campo por ID
     */
    Optional<FieldDefinition> getById(Integer id);

    /**
     * Obtiene una definición de campo por código
     */
    Optional<FieldDefinition> getByFieldCode(String fieldCode);

    /**
     * Cuenta el total de campos activos
     */
    long countActiveFields();

    /**
     * Obtiene campos filtrados por tabla asociada
     */
    List<FieldDefinition> getByAssociatedTable(String tableName);

    /**
     * Obtiene campos filtrados por múltiples tablas asociadas
     */
    List<FieldDefinition> getByAssociatedTables(List<String> tableNames);

    /**
     * Obtiene campos para sincronización de clientes (clientes + metodos_contacto)
     */
    List<FieldDefinition> getFieldsForCustomerSync();
}
