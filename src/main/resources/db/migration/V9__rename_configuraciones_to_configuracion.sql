-- Migration: Rename table configuraciones_importacion to configuracion_importacion
-- Purpose: Change plural form to singular for consistency

-- Rename the table
ALTER TABLE configuraciones_importacion RENAME TO configuracion_importacion;
