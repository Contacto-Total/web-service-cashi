-- ========================================
-- MIGRACIÓN V15: Tablas de validación Osiptel (Fase 1 - standalone)
--
-- Persiste resultados de la validación de titularidad consultada en
-- el portal `checatuslineas.osiptel.gob.pe` mediante worker Node.js
-- (Playwright + 2Captcha v3).
--
-- IMPORTANTE: el portal funciona POR DOCUMENTO, no por teléfono.
-- Una consulta = un documento (DNI/CE/Pasaporte/RUC) -> lista de líneas
-- con teléfono parcialmente enmascarado (5 dígitos visibles + 4 con '*').
--
-- Modelo:
--  - osiptel_validation_log: 1 fila por DNI consultado
--  - osiptel_phone_match: derivado, 1 fila por (validación, teléfono del cliente
--    cuyo prefijo se cruzó con las líneas devueltas)
--  - osiptel_validation_attempt: historial de intentos del worker
--  - osiptel_validation_archive: archive nocturno
--
-- Cumple Ley 29733: NO guarda nombre del titular, NO guarda DNI plaintext.
-- Solo dni_hash (SHA-256 con sal global) + match boolean + operator público.
-- ========================================

-- ============================
-- 1. Tabla principal: osiptel_validation_log (1 fila por DNI consultado)
-- ============================
CREATE TABLE IF NOT EXISTS osiptel_validation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,

    -- Identidad del sujeto consultado (documento, no teléfono)
    dni_hash CHAR(64) NOT NULL COMMENT 'SHA-256(dni + salt). Nunca DNI plaintext.',
    dni_type VARCHAR(20) NOT NULL DEFAULT 'DNI' COMMENT 'DNI|CE|PASAPORTE|RUC',

    -- Resultado: lista de líneas devueltas por el portal (operador + prefijo + modalidad)
    lines_json TEXT DEFAULT NULL COMMENT 'JSON: [{"phonePrefix":"97851","operator":"MOVISTAR","modality":"CONTROL"}]',
    lines_count SMALLINT NOT NULL DEFAULT 0 COMMENT 'Cantidad de lineas devueltas (filtro rapido)',

    -- Estado del ciclo de vida
    status VARCHAR(20) NOT NULL COMMENT 'PENDING|IN_PROGRESS|OK|NOT_FOUND|FAILED|EXPIRED',
    attempts SMALLINT NOT NULL DEFAULT 0,
    last_error VARCHAR(120) DEFAULT NULL,

    -- Timestamps operacionales
    enqueued_at DATETIME NOT NULL,
    started_at DATETIME DEFAULT NULL,
    finished_at DATETIME DEFAULT NULL,
    cooldown_until DATETIME DEFAULT NULL COMMENT 'No re-validar antes de esta fecha',

    -- Trazabilidad de origen (que cliente del CRM disparo esta validacion)
    source_customer_id BIGINT DEFAULT NULL,
    source_subportfolio_id BIGINT DEFAULT NULL,
    worker_id VARCHAR(40) DEFAULT NULL,

    -- Auditoria compatible con AggregateRoot
    fecha_creacion DATETIME DEFAULT NULL,
    fecha_actualizacion DATETIME DEFAULT NULL,

    PRIMARY KEY (id),

    -- Idempotencia: bloquea encolar PENDING o IN_PROGRESS duplicado para el mismo DNI.
    -- Cuando status pasa a terminal, libera el slot para una nueva validacion posterior.
    UNIQUE KEY uk_osiptel_dni_active (dni_hash, status),

    -- Claim del dispatcher (SELECT ... FOR UPDATE SKIP LOCKED)
    KEY idx_osiptel_status_enqueued (status, enqueued_at),

    -- Lookup del historico de un cliente
    KEY idx_osiptel_customer_finished (source_customer_id, finished_at),

    -- Metricas por subcartera (referencia logica - sin FK porque
    -- subcarteras.id es INT mientras que clientes.id_subcartera ya es
    -- BIGINT sin enforcement; replicamos ese patron del proyecto)
    KEY idx_osiptel_subportfolio_status (source_subportfolio_id, status),

    CONSTRAINT fk_osiptel_customer
        FOREIGN KEY (source_customer_id)
        REFERENCES clientes(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================
-- 2. Tabla derivada: osiptel_phone_match
-- Se llena durante recordResult: por cada telefono del cliente se evalua
-- si el prefijo (primeros 5 digitos) coincide con alguna linea del portal.
-- match=true es probabilistico (10000 telefonos comparten un prefijo de 5)
-- pero suficiente para cobranza.
-- ============================
CREATE TABLE IF NOT EXISTS osiptel_phone_match (
    id BIGINT NOT NULL AUTO_INCREMENT,
    validation_id BIGINT NOT NULL,

    phone VARCHAR(15) NOT NULL COMMENT 'Movil PE (9XXXXXXXX) del cliente, sin enmascarar',
    phone_prefix CHAR(5) NOT NULL COMMENT 'Primeros 5 digitos (los visibles en el portal)',

    dni_match TINYINT(1) NOT NULL COMMENT '1=prefijo encontrado en lineas_json, 0=no encontrado',
    matched_operator VARCHAR(20) DEFAULT NULL,
    matched_modality VARCHAR(20) DEFAULT NULL,

    fecha_creacion DATETIME DEFAULT NULL,

    PRIMARY KEY (id),
    KEY idx_phone_match_phone (phone, fecha_creacion),
    KEY idx_phone_match_validation (validation_id),

    CONSTRAINT fk_phone_match_validation
        FOREIGN KEY (validation_id)
        REFERENCES osiptel_validation_log(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================
-- 3. Historial detallado por intento: osiptel_validation_attempt
-- ============================
CREATE TABLE IF NOT EXISTS osiptel_validation_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    validation_id BIGINT NOT NULL,

    attempt_no SMALLINT NOT NULL,
    http_status SMALLINT DEFAULT NULL,
    captcha_attempts SMALLINT NOT NULL DEFAULT 0,
    latency_ms INT DEFAULT NULL,
    result_status VARCHAR(20) NOT NULL,
    error_detail VARCHAR(255) DEFAULT NULL,
    worker_id VARCHAR(40) DEFAULT NULL,

    fecha_creacion DATETIME DEFAULT NULL,

    PRIMARY KEY (id),
    KEY idx_attempt_validation (validation_id, attempt_no),

    CONSTRAINT fk_attempt_validation
        FOREIGN KEY (validation_id)
        REFERENCES osiptel_validation_log(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================
-- 4. Tabla de archivado (la rellena el SP de V16)
-- ============================
CREATE TABLE IF NOT EXISTS osiptel_validation_archive (
    id BIGINT NOT NULL,
    dni_hash CHAR(64) NOT NULL,
    dni_type VARCHAR(20) NOT NULL DEFAULT 'DNI',
    lines_count SMALLINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    attempts SMALLINT NOT NULL DEFAULT 0,
    enqueued_at DATETIME NOT NULL,
    started_at DATETIME DEFAULT NULL,
    finished_at DATETIME DEFAULT NULL,
    source_customer_id BIGINT DEFAULT NULL,
    source_subportfolio_id BIGINT DEFAULT NULL,
    worker_id VARCHAR(40) DEFAULT NULL,
    fecha_creacion DATETIME DEFAULT NULL,
    fecha_archivado DATETIME DEFAULT NULL,

    PRIMARY KEY (id),
    KEY idx_archive_finished (finished_at),
    KEY idx_archive_customer (source_customer_id),
    KEY idx_archive_subportfolio (source_subportfolio_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
