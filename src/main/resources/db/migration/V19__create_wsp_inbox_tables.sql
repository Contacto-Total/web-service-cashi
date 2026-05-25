-- ============================================================
-- V19: Tablas para el inbox compartido de WhatsApp
--
-- Modelo: una sesión WhatsApp, múltiples asesoras ven todo.
-- wsp_usuarios      → quién puede operar el inbox
-- wsp_conversaciones → un hilo por JID de cliente
-- wsp_mensajes      → mensajes del hilo; asesora_id indica quién respondió
-- ============================================================

-- Asesoras y supervisores del inbox
CREATE TABLE cashi_db.wsp_usuarios (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    nombre    VARCHAR(100) NOT NULL,
    email     VARCHAR(150) NOT NULL,
    rol       ENUM('ASESORA', 'SUPERVISOR') NOT NULL DEFAULT 'ASESORA',
    activa    TINYINT(1)   NOT NULL DEFAULT 1,
    creado_en TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wsp_usuarios_email (email)
);

-- Un hilo de conversación por cliente (JID único)
CREATE TABLE cashi_db.wsp_conversaciones (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    chat_jid         VARCHAR(100) NOT NULL,
    chat_titulo      VARCHAR(200),
    estado           ENUM('ACTIVA', 'CERRADA') NOT NULL DEFAULT 'ACTIVA',
    creada_en        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_actividad TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY  uk_wsp_conv_jid           (chat_jid),
    INDEX       idx_wsp_conv_estado       (estado),
    INDEX       idx_wsp_conv_actividad    (ultima_actividad)
);

-- Mensajes de cada conversación
CREATE TABLE cashi_db.wsp_mensajes (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    conversacion_id BIGINT       NOT NULL,
    wsp_msg_id      VARCHAR(100) NOT NULL,
    direccion       ENUM('ENTRANTE', 'SALIENTE') NOT NULL,
    texto           TEXT,
    tiene_media     TINYINT(1)   NOT NULL DEFAULT 0,
    media_tipo      VARCHAR(20),           -- image | video | audio | document | sticker
    media_url       VARCHAR(500),          -- URL relativa al Go service  (/media/{id})
    media_mime      VARCHAR(100),
    media_nombre    VARCHAR(255),
    quoted_msg_id   VARCHAR(100),
    quoted_texto    TEXT,
    asesora_id      BIGINT,                -- NULL si ENTRANTE; ID de quien respondió si SALIENTE
    estado_entrega  ENUM('PENDIENTE', 'ENVIADO', 'ENTREGADO', 'LEIDO') NOT NULL DEFAULT 'PENDIENTE',
    timestamp_wsp   BIGINT       NOT NULL, -- Unix timestamp del mensaje en WhatsApp
    creado_en       TIMESTAMP    NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY  uk_wsp_mensajes_msg_id  (wsp_msg_id),
    CONSTRAINT  fk_wsp_msg_conv         FOREIGN KEY (conversacion_id) REFERENCES cashi_db.wsp_conversaciones (id),
    CONSTRAINT  fk_wsp_msg_asesora      FOREIGN KEY (asesora_id)      REFERENCES cashi_db.wsp_usuarios (id),
    INDEX       idx_wsp_msg_conv        (conversacion_id),
    INDEX       idx_wsp_msg_timestamp   (timestamp_wsp)
);
