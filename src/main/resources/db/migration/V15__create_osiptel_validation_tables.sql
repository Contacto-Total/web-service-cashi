-- ========================================
-- MIGRACIÓN V15: Tablas de validación Osiptel (Fase 1 - standalone)
--
-- Persiste resultados de la validación de titularidad consultada en
-- el portal `checatuslineas.osiptel.gob.pe` mediante worker Node.js
-- (Playwright + 2Captcha).
--
-- NO acopla al modelo ortogonal de metodos_contacto (Fase 3 lo importará).
-- Cumple Ley 29733: NO guarda nombre del titular, NO guarda DNI plaintext.
-- Solo persiste: dni_match (boolean), operator (público), dni_hash (SHA-256).
-- ========================================

-- ============================
-- 1. Tabla principal: osiptel_validation_log
-- ============================
CREATE TABLE IF NOT EXISTS osiptel_validation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,

    -- Identificación del intento
    phone VARCHAR(15) NOT NULL COMMENT 'Móvil PE (9XXXXXXXX, sin código país)',
    dni_hash CHAR(64) DEFAULT NULL COMMENT 'SHA-256(dni + tenant_salt). Nunca DNI plaintext.',

    -- Resultado de la validación (lo único que se persiste del titular)
    dni_match TINYINT(1) DEFAULT NULL COMMENT '1=titular confirmado, 0=titular distinto, NULL=indeterminado',
    operator VARCHAR(20) DEFAULT NULL COMMENT 'CLARO|MOVISTAR|ENTEL|BITEL|OTRO',

    -- Estado del ciclo de vida
    status VARCHAR(20) NOT NULL COMMENT 'PENDING|IN_PROGRESS|OK|NOT_FOUND|FAILED|EXPIRED',
    attempts SMALLINT NOT NULL DEFAULT 0,
    last_error VARCHAR(120) DEFAULT NULL,

    -- Timestamps operacionales
    enqueued_at DATETIME NOT NULL,
    started_at DATETIME DEFAULT NULL,
    finished_at DATETIME DEFAULT NULL,
    cooldown_until DATETIME DEFAULT NULL COMMENT 'No re-validar antes de esta fecha',

    -- Trazabilidad de origen
    source_subportfolio_id BIGINT DEFAULT NULL,
    source_contact_method_id BIGINT DEFAULT NULL,
    worker_id VARCHAR(40) DEFAULT NULL,

    -- Auditoría compatible con AggregateRoot
    fecha_creacion DATETIME DEFAULT NULL,
    fecha_actualizacion DATETIME DEFAULT NULL,

    PRIMARY KEY (id),

    -- Idempotencia: bloquea encolar PENDING o IN_PROGRESS duplicado para el mismo número.
    -- Cuando status pasa a OK/FAILED/EXPIRED, libera el slot para una nueva validación posterior.
    UNIQUE KEY uk_osiptel_phone_status (phone, status),

    -- Claim rápido del dispatcher (SELECT ... FOR UPDATE SKIP LOCKED)
    KEY idx_osiptel_status_enqueued (status, enqueued_at),

    -- Lookup por número desde el frontend (GET /validations/{phone})
    KEY idx_osiptel_phone_finished (phone, finished_at),

    -- Métricas agregadas por subcartera
    KEY idx_osiptel_subportfolio_status (source_subportfolio_id, status),

    CONSTRAINT fk_osiptel_subportfolio
        FOREIGN KEY (source_subportfolio_id)
        REFERENCES subcarteras(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_osiptel_metodo_contacto
        FOREIGN KEY (source_contact_method_id)
        REFERENCES metodos_contacto(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================
-- 2. Historial por intento: osiptel_validation_attempt
-- Soporte de debugging, métricas detalladas y SLO del worker.
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
-- 3. Tabla de archivado (la rellena el SP de V16)
-- ============================
CREATE TABLE IF NOT EXISTS osiptel_validation_archive (
    id BIGINT NOT NULL,
    phone VARCHAR(15) NOT NULL,
    dni_hash CHAR(64) DEFAULT NULL,
    dni_match TINYINT(1) DEFAULT NULL,
    operator VARCHAR(20) DEFAULT NULL,
    status VARCHAR(20) NOT NULL,
    attempts SMALLINT NOT NULL DEFAULT 0,
    enqueued_at DATETIME NOT NULL,
    started_at DATETIME DEFAULT NULL,
    finished_at DATETIME DEFAULT NULL,
    source_subportfolio_id BIGINT DEFAULT NULL,
    worker_id VARCHAR(40) DEFAULT NULL,
    fecha_creacion DATETIME DEFAULT NULL,
    fecha_archivado DATETIME DEFAULT NULL,

    PRIMARY KEY (id),
    KEY idx_archive_finished (finished_at),
    KEY idx_archive_subportfolio (source_subportfolio_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
