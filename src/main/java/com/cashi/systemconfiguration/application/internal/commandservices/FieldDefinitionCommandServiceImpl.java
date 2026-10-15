package com.cashi.systemconfiguration.application.internal.commandservices;

import com.cashi.shared.domain.model.entities.FieldDefinition;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.FieldDefinitionRepository;
import com.cashi.systemconfiguration.domain.services.FieldDefinitionCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Set;

/**
 * Crea datos del catálogo. Un dato nuevo no tiene tabla asociada: no se copia a la ficha del cliente y
 * ninguna regla del sistema lo busca por código; sirve para enlazar una cabecera y usarla como filtro.
 */
@Service
public class FieldDefinitionCommandServiceImpl implements FieldDefinitionCommandService {

    private static final Set<String> TIPOS = Set.of("TEXTO", "NUMERICO", "FECHA", "BOOLEANO");

    private final FieldDefinitionRepository fieldDefinitionRepository;

    public FieldDefinitionCommandServiceImpl(FieldDefinitionRepository fieldDefinitionRepository) {
        this.fieldDefinitionRepository = fieldDefinitionRepository;
    }

    @Override
    @Transactional
    public FieldDefinition crear(String nombre, String tipoDato) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre del dato es obligatorio");
        }
        String tipo = tipoDato == null ? "" : tipoDato.trim().toUpperCase();
        if (!TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de dato no válido: " + tipoDato);
        }
        String codigo = codigoDe(limpio);
        if (codigo.isEmpty()) {
            throw new IllegalArgumentException("El nombre del dato debe tener letras o números");
        }
        if (fieldDefinitionRepository.existsByFieldCode(codigo)) {
            throw new IllegalArgumentException("Ya existe un dato con ese nombre (" + codigo + ")");
        }
        return fieldDefinitionRepository.save(new FieldDefinition(codigo, limpio, null, tipo, null));
    }

    /** «Rango de maduración» → rango_de_maduracion. */
    static String codigoDe(String nombre) {
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }
}
