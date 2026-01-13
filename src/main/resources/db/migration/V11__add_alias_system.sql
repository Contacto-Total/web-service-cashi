-- ========================================
-- MIGRACIÓN V11: Sistema de Alias y Auto-detección de Cabeceras
-- ========================================

-- 1. Tabla de alias para cabeceras
-- Permite que una cabecera tenga múltiples nombres alternativos
CREATE TABLE alias_cabeceras (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_configuracion_cabecera INT NOT NULL,
    alias VARCHAR(100) NOT NULL COMMENT 'Nombre alternativo de la cabecera',
    es_principal INT DEFAULT 0 COMMENT '1 = nombre principal, 0 = alias',
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_configuracion_cabecera)
        REFERENCES configuracion_cabeceras(id) ON DELETE CASCADE,

    -- Un alias debe ser único dentro de la misma subcartera y tipo de carga
    -- Esto se validará a nivel de aplicación
    INDEX idx_alias_config (id_configuracion_cabecera),
    INDEX idx_alias_nombre (alias)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Alias (nombres alternativos) para cabeceras de configuración';

-- 2. Agregar columnas a configuracion_cabeceras para auto-detección
ALTER TABLE configuracion_cabeceras
    ADD COLUMN auto_agregar_nuevas INT DEFAULT 0
        COMMENT '1 = agregar automáticamente columnas nuevas detectadas',
    ADD COLUMN columnas_ignoradas JSON DEFAULT NULL
        COMMENT 'Lista de nombres de columnas a ignorar en importación';

-- 3. Tabla de historial de cambios de cabeceras (auditoría)
CREATE TABLE historial_cambios_cabeceras (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_subcartera INT NOT NULL,
    tipo_carga VARCHAR(20) NOT NULL COMMENT 'INICIAL o ACTUALIZACION',
    tipo_cambio VARCHAR(50) NOT NULL COMMENT 'ALIAS_AGREGADO, COLUMNA_NUEVA, COLUMNA_IGNORADA, ALIAS_REMOVIDO',
    nombre_columna_excel VARCHAR(100) NOT NULL COMMENT 'Nombre de la columna en el Excel',
    nombre_cabecera_interna VARCHAR(100) COMMENT 'Nombre interno de la cabecera (si aplica)',
    id_configuracion_cabecera INT COMMENT 'FK a la cabecera afectada (si aplica)',
    usuario VARCHAR(100) COMMENT 'Usuario que realizó el cambio',
    fecha_cambio DATETIME DEFAULT CURRENT_TIMESTAMP,
    metadata JSON COMMENT 'Información adicional del cambio',

    INDEX idx_historial_subcartera (id_subcartera),
    INDEX idx_historial_fecha (fecha_cambio DESC),
    INDEX idx_historial_tipo (tipo_cambio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Historial de cambios en configuración de cabeceras';

-- 4. Tabla de tipos de archivo complementario
-- Define los tipos de archivos adicionales que pueden cargarse
CREATE TABLE tipos_archivo_complementario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_subcartera INT NOT NULL,
    nombre_tipo VARCHAR(50) NOT NULL COMMENT 'Ej: FACILIDADES, PKM',
    patron_nombre VARCHAR(200) NOT NULL COMMENT 'Patrón regex para identificar el archivo',
    campo_vinculacion VARCHAR(100) NOT NULL COMMENT 'Campo para hacer match (ej: IDENTITY_CODE)',
    columnas_actualizar JSON NOT NULL COMMENT 'Lista de columnas que este archivo actualiza',
    descripcion VARCHAR(255),
    esta_activo INT DEFAULT 1,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (id_subcartera) REFERENCES subcarteras(id) ON DELETE CASCADE,

    UNIQUE INDEX idx_tipo_archivo_unico (id_subcartera, nombre_tipo),
    INDEX idx_tipo_archivo_subcartera (id_subcartera)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tipos de archivos complementarios por subcartera';

-- 5. Insertar alias principal para cabeceras existentes
-- Esto asegura que las cabeceras existentes tengan al menos su nombre como alias principal
INSERT INTO alias_cabeceras (id_configuracion_cabecera, alias, es_principal)
SELECT id, nombre_cabecera, 1
FROM configuracion_cabeceras
WHERE id NOT IN (SELECT id_configuracion_cabecera FROM alias_cabeceras);

-- 6. Índice para búsqueda rápida de alias por subcartera
CREATE INDEX idx_alias_busqueda ON alias_cabeceras (alias);
