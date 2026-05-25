-- Renombra los valores de estado_osiptel y estado_whatsapp al nuevo vocabulario.
-- Columnas son ENUM: hay que ampliar primero, UPDATE, luego contraer.

-- Paso 1: ampliar el ENUM para admitir valores viejos Y nuevos simultáneamente
ALTER TABLE cashi_db.metodos_contacto
    MODIFY COLUMN estado_osiptel  ENUM('SIN_VALIDAR','VALIDADO','NO_VALIDADO','PERTENECE','NO_PERTENECE') NOT NULL DEFAULT 'SIN_VALIDAR',
    MODIFY COLUMN estado_whatsapp ENUM('SIN_VALIDAR','VALIDADO','NO_VALIDADO','TIENE','NO_TIENE')         NOT NULL DEFAULT 'SIN_VALIDAR';

-- Paso 2: renombrar filas existentes
UPDATE cashi_db.metodos_contacto SET estado_osiptel  = 'PERTENECE'    WHERE estado_osiptel  = 'VALIDADO';
UPDATE cashi_db.metodos_contacto SET estado_osiptel  = 'NO_PERTENECE' WHERE estado_osiptel  = 'NO_VALIDADO';
UPDATE cashi_db.metodos_contacto SET estado_whatsapp = 'TIENE'        WHERE estado_whatsapp = 'VALIDADO';
UPDATE cashi_db.metodos_contacto SET estado_whatsapp = 'NO_TIENE'     WHERE estado_whatsapp = 'NO_VALIDADO';

-- Paso 3: contraer el ENUM a solo los valores nuevos
ALTER TABLE cashi_db.metodos_contacto
    MODIFY COLUMN estado_osiptel  ENUM('SIN_VALIDAR','PERTENECE','NO_PERTENECE') NOT NULL DEFAULT 'SIN_VALIDAR',
    MODIFY COLUMN estado_whatsapp ENUM('SIN_VALIDAR','TIENE','NO_TIENE')         NOT NULL DEFAULT 'SIN_VALIDAR';
