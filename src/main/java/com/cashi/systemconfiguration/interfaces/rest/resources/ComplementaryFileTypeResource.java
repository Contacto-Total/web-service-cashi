package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.entities.ComplementaryFileType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Recurso de respuesta para tipos de archivo complementario
 */
public record ComplementaryFileTypeResource(
    Integer id,
    Integer subPortfolioId,
    String subPortfolioName,
    String typeName,
    String fileNamePattern,
    String linkField,
    List<String> columnsToUpdate,
    String description,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convierte una entidad a recurso
     */
    public static ComplementaryFileTypeResource fromEntity(ComplementaryFileType entity) {
        List<String> columns;
        try {
            columns = objectMapper.readValue(entity.getColumnsToUpdate(), new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            columns = Collections.emptyList();
        }

        return new ComplementaryFileTypeResource(
            entity.getId(),
            entity.getSubPortfolio().getId(),
            entity.getSubPortfolio().getSubPortfolioName(),
            entity.getTypeName(),
            entity.getFileNamePattern(),
            entity.getLinkField(),
            columns,
            entity.getDescription(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
