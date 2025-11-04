-- Migration: Drop field transformation rules table
-- Purpose: Remove reglas_configuracion_cabeceras as we now use a different strategy

-- Drop the table
DROP TABLE IF EXISTS reglas_configuracion_cabeceras;
