package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.FieldDefinition;

/**
 * Alta de datos en el catálogo de campos (definiciones_campos) desde la configuración de cabeceras.
 */
public interface FieldDefinitionCommandService {

    /**
     * Crea un dato del catálogo con su nombre y tipo. El código se arma con el nombre.
     */
    FieldDefinition crear(String nombre, String tipoDato);
}
