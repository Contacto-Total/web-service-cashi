-- V5: Create tables for automatic file import functionality

-- Table: configuraciones_importacion
CREATE TABLE configuraciones_importacion (
    id_configuracion BIGINT AUTO_INCREMENT PRIMARY KEY,
    directorio_monitoreado VARCHAR(500) NOT NULL COMMENT 'Watch directory path',
    patron_archivo VARCHAR(255) NOT NULL COMMENT 'File pattern to match',
    id_subcartera INT NOT NULL COMMENT 'Sub-portfolio ID to import data into',
    frecuencia_revision_minutos INT NOT NULL DEFAULT 15 COMMENT 'Check frequency in minutes',
    activo BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether monitoring is active',
    directorio_procesados VARCHAR(500) NOT NULL COMMENT 'Directory for processed files',
    directorio_errores VARCHAR(500) NOT NULL COMMENT 'Directory for error files',
    mover_despues_procesar BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Move files after processing',
    ultima_revision DATETIME COMMENT 'Last check timestamp',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Configuration for automatic file import';

-- Table: historial_importaciones
CREATE TABLE historial_importaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subcartera_id BIGINT COMMENT 'Sub-portfolio ID if applicable',
    nombre_archivo VARCHAR(255) NOT NULL COMMENT 'File name',
    ruta_archivo VARCHAR(500) NOT NULL COMMENT 'Full file path',
    procesado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Processing timestamp',
    estado VARCHAR(20) NOT NULL COMMENT 'SUCCESS or ERROR',
    registros_procesados INT DEFAULT 0 COMMENT 'Number of records processed',
    mensaje_error TEXT COMMENT 'Error message if failed',
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='History of imported files';

-- Indexes for better query performance
CREATE INDEX idx_historial_subcartera ON historial_importaciones(subcartera_id);
CREATE INDEX idx_historial_procesado_en ON historial_importaciones(procesado_en DESC);
CREATE INDEX idx_historial_estado ON historial_importaciones(estado);
CREATE INDEX idx_historial_ruta_estado ON historial_importaciones(ruta_archivo(255), estado);

-- Index for configuration active status
CREATE INDEX idx_config_activo ON configuraciones_importacion(activo);
