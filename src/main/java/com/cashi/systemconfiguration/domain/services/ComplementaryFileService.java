package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.ComplementaryFileType;

import java.util.List;
import java.util.Map;

/**
 * Servicio para gestión de archivos complementarios.
 * Los archivos complementarios son aquellos que actualizan columnas específicas
 * de la tabla principal usando un campo de vinculación (ej: IDENTITY_CODE).
 */
public interface ComplementaryFileService {

    /**
     * Importa datos desde un archivo complementario.
     * Realiza UPDATE sobre la tabla de carga inicial usando el campo de vinculación.
     *
     * @param subPortfolioId ID de la subcartera
     * @param complementaryTypeId ID del tipo de archivo complementario
     * @param data Lista de filas a procesar
     * @return Resultado de la importación con contadores y errores
     */
    Map<String, Object> importComplementaryData(Integer subPortfolioId, Integer complementaryTypeId, List<Map<String, Object>> data);

    /**
     * Importa datos complementarios usando el nombre del tipo en lugar del ID.
     *
     * @param subPortfolioId ID de la subcartera
     * @param typeName Nombre del tipo (ej: "FACILIDADES", "PKM")
     * @param data Lista de filas a procesar
     * @return Resultado de la importación
     */
    Map<String, Object> importComplementaryDataByTypeName(Integer subPortfolioId, String typeName, List<Map<String, Object>> data);

    // ==================== CRUD de Tipos de Archivo Complementario ====================

    /**
     * Obtiene todos los tipos de archivo complementario de una subcartera.
     */
    List<ComplementaryFileType> getComplementaryTypesBySubPortfolio(Integer subPortfolioId);

    /**
     * Obtiene un tipo de archivo complementario por ID.
     */
    ComplementaryFileType getComplementaryTypeById(Integer id);

    /**
     * Crea un nuevo tipo de archivo complementario.
     */
    ComplementaryFileType createComplementaryType(Integer subPortfolioId, String typeName, String filePattern,
                                                   String linkField, List<String> columnsToUpdate, String description);

    /**
     * Actualiza un tipo de archivo complementario.
     */
    ComplementaryFileType updateComplementaryType(Integer id, String typeName, String filePattern,
                                                   String linkField, List<String> columnsToUpdate,
                                                   String description, Boolean isActive);

    /**
     * Elimina un tipo de archivo complementario.
     */
    void deleteComplementaryType(Integer id);

    /**
     * Detecta el tipo de archivo complementario basándose en el nombre del archivo.
     *
     * @param subPortfolioId ID de la subcartera
     * @param fileName Nombre del archivo
     * @return Tipo detectado o null si no coincide con ningún patrón
     */
    ComplementaryFileType detectComplementaryType(Integer subPortfolioId, String fileName);

    /**
     * Valida que las columnas a actualizar existan en la configuración de cabeceras.
     *
     * @param subPortfolioId ID de la subcartera
     * @param columns Columnas a validar
     * @return Lista de columnas que no existen en la configuración
     */
    List<String> validateColumnsExist(Integer subPortfolioId, List<String> columns);
}
