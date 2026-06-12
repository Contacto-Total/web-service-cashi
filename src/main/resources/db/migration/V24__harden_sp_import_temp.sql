-- ============================================================
-- V24 — Endurecimiento de atomicidad de los SP de carga consolidada.
--
-- Cambio vs V23: el staging pasa a ser TABLA TEMPORARY (creada por Java).
-- CREATE/DROP TEMPORARY TABLE NO provocan commit implícito (a diferencia de
-- CREATE/DROP TABLE), por lo que TODO el flujo (writes del SP + sync de clientes)
-- queda dentro de la transacción @Transactional de Spring y revierte limpio
-- ante cualquier fallo.
--
-- Como las tablas TEMPORARY no aparecen en information_schema, el SP ya NO
-- introspecciona columnas: Java pasa las listas/fragmentos SQL ya armados
-- (columnas y SET clause), con nombres sanitizados por SqlSanitizer.
-- Esto además elimina la dependencia de group_concat_max_len.
--
-- Validado contra foh_cas_cst real (QAS) de forma reversible: upsert con TEMPORARY
-- + ROLLBACK revierte las escrituras del destino (atomicidad efectiva).
-- ============================================================

DROP PROCEDURE IF EXISTS sp_import_upsert;
DROP PROCEDURE IF EXISTS sp_import_update;

DELIMITER //

-- INICIAL y ACTUALIZACION (Fase 1 diaria).
--   p_cols : "`col1`, `col2`, ..."                        (lista de columnas de datos)
--   p_set  : "d.`col1`=s.`col1`, d.`col2`=s.`col2`, ..."  (SET para el UPDATE)
CREATE PROCEDURE sp_import_upsert(
  IN  p_staging  VARCHAR(128),
  IN  p_dest     VARCHAR(128),
  IN  p_cols     TEXT,
  IN  p_set      TEXT,
  OUT p_inserted INT,
  OUT p_updated  INT
)
BEGIN
  SET @sql_u = CONCAT('UPDATE `', p_dest, '` d JOIN `', p_staging,
                      '` s ON d.id = s.__existing_id SET ', p_set,
                      ' WHERE s.__existing_id IS NOT NULL');
  PREPARE st FROM @sql_u; EXECUTE st; SET p_updated = ROW_COUNT(); DEALLOCATE PREPARE st;

  SET @sql_i = CONCAT('INSERT INTO `', p_dest, '` (', p_cols, ') SELECT ', p_cols,
                      ' FROM `', p_staging, '` WHERE __existing_id IS NULL');
  PREPARE st FROM @sql_i; EXECUTE st; SET p_inserted = ROW_COUNT(); DEALLOCATE PREPARE st;
END //

-- Complementaria y Fase 2 diaria.
--   p_set : SET clause ya armado por Java. Para la diaria, Java usa
--           "d.`col`=COALESCE(s.`col`, d.`col`)"; para complementaria, "d.`col`=s.`col`".
CREATE PROCEDURE sp_import_update(
  IN  p_staging   VARCHAR(128),
  IN  p_dest      VARCHAR(128),
  IN  p_link_col  VARCHAR(128),
  IN  p_set       TEXT,
  OUT p_updated   INT,
  OUT p_not_found INT
)
BEGIN
  SET @sql_u = CONCAT('UPDATE `', p_dest, '` d JOIN `', p_staging,
                      '` s ON d.`', p_link_col, '` = s.__link SET ', p_set);
  PREPARE st FROM @sql_u; EXECUTE st; SET p_updated = ROW_COUNT(); DEALLOCATE PREPARE st;

  SET @sql_n = CONCAT('SELECT COUNT(*) INTO @nf FROM `', p_staging, '` s LEFT JOIN `', p_dest,
                      '` d ON d.`', p_link_col, '` = s.__link WHERE d.id IS NULL');
  PREPARE st FROM @sql_n; EXECUTE st; SET p_not_found = @nf; DEALLOCATE PREPARE st;
END //

DELIMITER ;
