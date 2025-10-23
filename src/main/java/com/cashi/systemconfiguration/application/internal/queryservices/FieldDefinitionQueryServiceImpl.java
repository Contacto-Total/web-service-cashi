package com.cashi.systemconfiguration.application.internal.queryservices;

import com.cashi.shared.domain.model.entities.FieldDefinition;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.FieldDefinitionRepository;
import com.cashi.systemconfiguration.domain.services.FieldDefinitionQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FieldDefinitionQueryServiceImpl implements FieldDefinitionQueryService {

    private final FieldDefinitionRepository fieldDefinitionRepository;

    public FieldDefinitionQueryServiceImpl(FieldDefinitionRepository fieldDefinitionRepository) {
        this.fieldDefinitionRepository = fieldDefinitionRepository;
    }

    @Override
    public List<FieldDefinition> getAllActive() {
        return fieldDefinitionRepository.findAllOrderedByName();
    }

    @Override
    public List<FieldDefinition> getAllActiveByCategory(String category) {
        // Ya no existe el concepto de categoría, devolver todos
        return fieldDefinitionRepository.findAllOrderedByName();
    }

    @Override
    public List<FieldDefinition> getAllActiveByDataType(String dataType) {
        String normalizedType = dataType.toUpperCase();
        if (!normalizedType.equals("TEXTO") && !normalizedType.equals("NUMERICO") && !normalizedType.equals("FECHA")) {
            throw new IllegalArgumentException("Tipo de dato inválido: " + dataType + ". Use: TEXTO, NUMERICO o FECHA");
        }
        return fieldDefinitionRepository.findByDataType(normalizedType);
    }

    @Override
    public Optional<FieldDefinition> getById(Integer id) {
        return fieldDefinitionRepository.findById(id);
    }

    @Override
    public Optional<FieldDefinition> getByFieldCode(String fieldCode) {
        return fieldDefinitionRepository.findByFieldCode(fieldCode);
    }

    @Override
    public long countActiveFields() {
        return fieldDefinitionRepository.count();
    }
}
