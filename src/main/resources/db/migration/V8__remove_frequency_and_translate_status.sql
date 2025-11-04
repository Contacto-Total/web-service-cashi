-- V8: Eliminar frecuencia_revision_minutos y traducir estados a español

-- 1. Actualizar estados existentes de inglés a español en historial_importaciones
UPDATE historial_importaciones
SET estado = 'EXITOSO'
WHERE estado = 'SUCCESS';

UPDATE historial_importaciones
SET estado = 'EXITOSO_CON_ERRORES'
WHERE estado = 'SUCCESS_WITH_ERRORS';

-- 2. Hacer hora_programada obligatoria (NOT NULL) con valor por defecto
UPDATE configuraciones_importacion
SET hora_programada = '02:00:00'
WHERE hora_programada IS NULL;

ALTER TABLE configuraciones_importacion
MODIFY COLUMN hora_programada VARCHAR(8) NOT NULL COMMENT 'Scheduled time for daily import in HH:mm:ss format (e.g., 02:00:00)';

-- 3. Eliminar columna frecuencia_revision_minutos (ya no se usa)
ALTER TABLE configuraciones_importacion
DROP COLUMN frecuencia_revision_minutos;

-- Nota: Los estados ahora son:
-- - EXITOSO: Importación completamente exitosa
-- - EXITOSO_CON_ERRORES: Importación parcial con algunos errores
-- - ERROR: Importación fallida
