-- =====================================================================
-- V17 — Pivot al modelo NO-ortogonal (estado_contacto unificado)
-- =====================================================================
-- 1) Drop de las 4 tablas Osiptel del enfoque viejo (V15/V16) + SP
-- 2) Nuevas columnas en metodos_contacto: estado_osiptel, estado_whatsapp, estado_contacto
-- 3) Nueva columna en catalogo_tipificaciones: estado_contacto_resultante
-- 4) Mapeo segun nuevoEnfoqueNoOrtogonico.xlsx (Hoja2)
-- 5) Backfill de metodos_contacto.estado_contacto desde la ultima tipificacion
--
-- Coexiste con estado_contactabilidad (NO se borra esa columna en esta migracion).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) DROP de las tablas Osiptel + stored procedure
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS cashi_db.osiptel_validation_archive;
DROP TABLE IF EXISTS cashi_db.osiptel_validation_attempt;
DROP TABLE IF EXISTS cashi_db.osiptel_phone_match;
DROP TABLE IF EXISTS cashi_db.osiptel_validation_log;
DROP PROCEDURE IF EXISTS cashi_db.sp_osiptel_archive;

-- ---------------------------------------------------------------------
-- 2) Columnas nuevas en metodos_contacto
-- ---------------------------------------------------------------------
-- estado_contactabilidad existente es VARCHAR(30) DEFAULT 'NUEVO' (V20260304_2).
-- Las nuevas columnas siguen el mismo patron: VARCHAR + DEFAULT, sin ENUM SQL,
-- el constraint de valores se aplica en Java con @Enumerated(EnumType.STRING).
ALTER TABLE cashi_db.metodos_contacto
    ADD COLUMN estado_osiptel  VARCHAR(20) NOT NULL DEFAULT 'SIN_VALIDAR'
        COMMENT 'SIN_VALIDAR|VALIDADO|NO_VALIDADO - resultado de validacion vs portal Osiptel',
    ADD COLUMN estado_whatsapp VARCHAR(20) NOT NULL DEFAULT 'SIN_VALIDAR'
        COMMENT 'SIN_VALIDAR|VALIDADO|NO_VALIDADO - resultado de validacion WhatsApp',
    ADD COLUMN estado_contacto VARCHAR(30) NOT NULL DEFAULT 'NUEVO'
        COMMENT 'NUEVO|CONTACTO_TITULAR|NO_CONTACTADO|CONTACTO_TERCERO|INVALIDO|INVALIDO_CONFIRMADO - estado unificado del contacto, derivado de la ultima tipificacion (agente o discador)';

CREATE INDEX idx_metodos_contacto_estado_contacto ON cashi_db.metodos_contacto (estado_contacto);
CREATE INDEX idx_metodos_contacto_estado_osiptel  ON cashi_db.metodos_contacto (estado_osiptel);

-- ---------------------------------------------------------------------
-- 3) Columna de mapeo en catalogo_tipificaciones
-- ---------------------------------------------------------------------
ALTER TABLE cashi_db.catalogo_tipificaciones
    ADD COLUMN estado_contacto_resultante VARCHAR(30) NULL
        COMMENT 'Si se registra esta tipificacion, metodos_contacto.estado_contacto pasa a este valor (respetando reglas sticky en el listener Java).';

-- ---------------------------------------------------------------------
-- 4) Poblar el mapeo segun Hoja2 del Excel
-- ---------------------------------------------------------------------
-- CONTACTO_TITULAR: tipificaciones que confirman al titular (OP, PP, RP, CA, CFA, CON, CCL, CIP, REG)
UPDATE cashi_db.catalogo_tipificaciones
SET estado_contacto_resultante = 'CONTACTO_TITULAR'
WHERE codigo IN ('OP', 'PP', 'RP', 'CA', 'CFA', 'CON', 'CCL', 'CIP', 'REG');

-- NO_CONTACTADO: agente (MV, AP, FS, SLT, CTL) + discador automatico (SYS_*)
UPDATE cashi_db.catalogo_tipificaciones
SET estado_contacto_resultante = 'NO_CONTACTADO'
WHERE codigo IN (
    'MV', 'AP', 'FS', 'SLT', 'CTL',
    'SYS_NO_CONTESTA', 'SYS_ABANDONADA', 'SYS_FALLIDA',
    'SYS_OCUPADO', 'SYS_BUZON_VOZ', 'SYS_SILENCIO'
);

-- CONTACTO_TERCERO: terceros (FAM, AMI, PAD, VEC, HME, HEN, CDT, COY, CNY)
UPDATE cashi_db.catalogo_tipificaciones
SET estado_contacto_resultante = 'CONTACTO_TERCERO'
WHERE codigo IN ('FAM', 'AMI', 'PAD', 'VEC', 'HME', 'HEN', 'CDT', 'COY', 'CNY');

-- INVALIDO: equivocado o fallecido
UPDATE cashi_db.catalogo_tipificaciones
SET estado_contacto_resultante = 'INVALIDO'
WHERE codigo IN ('EQI', 'FAL');

-- INVALIDO_CONFIRMADO: aun NO se modela (no se asignan tipificaciones a este estado)
-- Padres nivel 1 (CD, CI, NC) y nivel 2 sin mapeo (TAC, CT, NCO) quedan NULL: NO disparan cambio.

-- ---------------------------------------------------------------------
-- 5) Backfill: poblar metodos_contacto.estado_contacto desde la ultima
--    tipificacion historica de cada (telefono_contacto, id_cliente).
--
--    Conexion: registros_gestion.telefono_contacto = metodos_contacto.valor
--              AND registros_gestion.id_cliente   = metodos_contacto.id_cliente
--
--    Toma la tipificacion mas reciente (fecha_gestion DESC) que tenga
--    mapeo (estado_contacto_resultante IS NOT NULL).
-- ---------------------------------------------------------------------
UPDATE cashi_db.metodos_contacto mc
INNER JOIN (
    SELECT
        rg.telefono_contacto,
        rg.id_cliente,
        ct.estado_contacto_resultante,
        ROW_NUMBER() OVER (
            PARTITION BY rg.telefono_contacto, rg.id_cliente
            ORDER BY rg.fecha_gestion DESC, rg.id DESC
        ) AS rn
    FROM cashi_db.registros_gestion rg
    INNER JOIN cashi_db.catalogo_tipificaciones ct ON ct.id = rg.id_tipificacion
    WHERE ct.estado_contacto_resultante IS NOT NULL
      AND rg.telefono_contacto IS NOT NULL
      AND rg.id_cliente IS NOT NULL
) ult
    ON ult.telefono_contacto = mc.valor
   AND ult.id_cliente        = mc.id_cliente
   AND ult.rn                = 1
SET mc.estado_contacto = ult.estado_contacto_resultante
WHERE mc.estado_contacto = 'NUEVO';

-- ---------------------------------------------------------------------
-- Fin V17. Verificacion sugerida tras aplicar:
--   SELECT estado_contacto, COUNT(*) FROM cashi_db.metodos_contacto GROUP BY estado_contacto;
--   SELECT codigo, estado_contacto_resultante FROM cashi_db.catalogo_tipificaciones
--     WHERE estado_contacto_resultante IS NOT NULL ORDER BY estado_contacto_resultante, codigo;
-- ---------------------------------------------------------------------
