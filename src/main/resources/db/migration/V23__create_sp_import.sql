-- ============================================================
-- V23 — Stored procedures set-based para carga consolidada (motor SP).
-- Reemplazan los bucles fila-por-fila de HeaderConfigurationCommandServiceImpl.
-- Operan sobre una tabla STAGING (poblada por Java vía batchUpdate) y la tabla
-- dinámica destino (<prov>_<cart>_<sub> INICIAL / ini_<...> ACTUALIZACION).
--
-- Columnas de control de staging (excluidas por NOT LIKE '__%'):
--   __row INT, __existing_id BIGINT (upsert), __link VARCHAR (update).
-- Las columnas de datos de staging deben tener el MISMO tipo y COLACIÓN que el
-- destino (Java las crea heredando table_collation del destino) para que el JOIN
-- por link no choque entre tablas con colaciones distintas (unicode_ci vs 0900_ai_ci).
--
-- Verificados contra foh_cas_cst real (QAS) de forma reversible:
--   upsert -> insertados/actualizados OK; update -> COALESCE preserva NULL, not_found OK.
-- ============================================================

DROP PROCEDURE IF EXISTS sp_import_upsert;
DROP PROCEDURE IF EXISTS sp_import_update;

DELIMITER //

-- INICIAL y ACTUALIZACION (Fase 1 diaria): UPDATE de existentes (por __existing_id
-- precomputado en Java) + INSERT de nuevos. El matching identity->nombre queda en Java.
CREATE PROCEDURE sp_import_upsert(
  IN  p_staging  VARCHAR(128),
  IN  p_dest     VARCHAR(128),
  OUT p_inserted INT,
  OUT p_updated  INT
)
BEGIN
  DECLARE v_cols TEXT;
  DECLARE v_set  TEXT;
  -- Evitar truncamiento de GROUP_CONCAT con tablas de muchas columnas (default 1024).
  SET SESSION group_concat_max_len = 1000000;

  SELECT GROUP_CONCAT(CONCAT('`', column_name, '`') ORDER BY ordinal_position SEPARATOR ', ')
    INTO v_cols FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = p_staging AND column_name NOT LIKE '\_\_%';

  SELECT GROUP_CONCAT(CONCAT('d.`', column_name, '` = s.`', column_name, '`') ORDER BY ordinal_position SEPARATOR ', ')
    INTO v_set FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = p_staging AND column_name NOT LIKE '\_\_%';

  SET @sql_u = CONCAT('UPDATE `', p_dest, '` d JOIN `', p_staging,
                      '` s ON d.id = s.__existing_id SET ', v_set,
                      ' WHERE s.__existing_id IS NOT NULL');
  PREPARE st FROM @sql_u; EXECUTE st; SET p_updated = ROW_COUNT(); DEALLOCATE PREPARE st;

  SET @sql_i = CONCAT('INSERT INTO `', p_dest, '` (', v_cols, ') SELECT ', v_cols,
                      ' FROM `', p_staging, '` WHERE __existing_id IS NULL');
  PREPARE st FROM @sql_i; EXECUTE st; SET p_inserted = ROW_COUNT(); DEALLOCATE PREPARE st;
END //

-- Complementaria y Fase 2 diaria: UPDATE de columnas por link field.
-- p_use_coalesce=1 -> d.col = COALESCE(s.col, d.col) (preserva existente si nuevo NULL);
-- p_use_coalesce=0 -> d.col = s.col.
CREATE PROCEDURE sp_import_update(
  IN  p_staging      VARCHAR(128),
  IN  p_dest         VARCHAR(128),
  IN  p_link_col     VARCHAR(128),
  IN  p_use_coalesce TINYINT,
  OUT p_updated      INT,
  OUT p_not_found    INT
)
BEGIN
  DECLARE v_set TEXT;
  SET SESSION group_concat_max_len = 1000000;

  IF p_use_coalesce = 1 THEN
    SELECT GROUP_CONCAT(CONCAT('d.`', column_name, '` = COALESCE(s.`', column_name, '`, d.`', column_name, '`)')
                        ORDER BY ordinal_position SEPARATOR ', ')
      INTO v_set FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = p_staging AND column_name NOT LIKE '\_\_%';
  ELSE
    SELECT GROUP_CONCAT(CONCAT('d.`', column_name, '` = s.`', column_name, '`')
                        ORDER BY ordinal_position SEPARATOR ', ')
      INTO v_set FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = p_staging AND column_name NOT LIKE '\_\_%';
  END IF;

  SET @sql_u = CONCAT('UPDATE `', p_dest, '` d JOIN `', p_staging,
                      '` s ON d.`', p_link_col, '` = s.__link SET ', v_set);
  PREPARE st FROM @sql_u; EXECUTE st; SET p_updated = ROW_COUNT(); DEALLOCATE PREPARE st;

  SET @sql_n = CONCAT('SELECT COUNT(*) INTO @nf FROM `', p_staging, '` s LEFT JOIN `', p_dest,
                      '` d ON d.`', p_link_col, '` = s.__link WHERE d.id IS NULL');
  PREPARE st FROM @sql_n; EXECUTE st; SET p_not_found = @nf; DEALLOCATE PREPARE st;
END //

DELIMITER ;
