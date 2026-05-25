-- Agrega el identificador de instancia Go a cada conversación.
-- Permite saber qué proceso Go (número WhatsApp) maneja cada chat.
ALTER TABLE cashi_db.wsp_conversaciones
    ADD COLUMN instancia_id VARCHAR(50) NOT NULL DEFAULT '1' AFTER estado;

CREATE INDEX idx_wsp_conv_instancia ON cashi_db.wsp_conversaciones (instancia_id);
