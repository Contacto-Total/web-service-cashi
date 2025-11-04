-- V6: Add scheduled time field for time-of-day based import scheduling

ALTER TABLE configuraciones_importacion
ADD COLUMN hora_programada VARCHAR(8) COMMENT 'Scheduled time for daily import in HH:mm:ss format (e.g., 02:00:00)';

-- Add index for better query performance when filtering by scheduled time
CREATE INDEX idx_config_hora_programada ON configuraciones_importacion(hora_programada);
