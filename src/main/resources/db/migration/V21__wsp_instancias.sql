-- Registro de instancias Go (una por número WhatsApp).
-- base_url: dirección HTTP del proceso Go, ej: http://localhost:8090
CREATE TABLE cashi_db.wsp_instancias (
    id          VARCHAR(50)  NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    base_url    VARCHAR(200) NOT NULL,
    activa      TINYINT(1)   NOT NULL DEFAULT 1,
    creada_en   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- Instancia por defecto (instancia "1" en puerto 8090)
INSERT INTO cashi_db.wsp_instancias (id, nombre, base_url)
VALUES ('1', 'WhatsApp Principal', 'http://localhost:8090');
