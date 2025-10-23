-- Migración V4: Permitir campos personalizados en configuración de cabeceras
-- Permite que id_definicion_campo sea NULL para campos que no están en el catálogo

-- Modificar la columna id_definicion_campo para permitir NULL
ALTER TABLE configuracion_cabeceras
    MODIFY COLUMN id_definicion_campo INTEGER NULL;

-- Actualizar comentario de la tabla
ALTER TABLE configuracion_cabeceras
    COMMENT = 'Configuración de cabeceras personalizadas por subcartera. Soporta campos vinculados al catálogo (id_definicion_campo) y campos personalizados (id_definicion_campo = NULL)';
