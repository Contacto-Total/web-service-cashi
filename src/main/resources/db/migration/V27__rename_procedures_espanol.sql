-- ============================================================
-- V27 — Renombrar los SP de la Carga Consolidada a español.
--
--   sp_import_upsert    -> sp_importar_upsert
--   sp_import_update    -> sp_importar_actualizar
--   sp_sync_customers   -> sp_sincronizar_clientes   (definido ya con el
--                          nombre nuevo en V26; aquí solo se dropea el viejo)
--
-- Los cuerpos de sp_importar_upsert / sp_importar_actualizar son idénticos a
-- los de V25 (6 params, SQL_SAFE_UPDATES save/restore); solo cambia el nombre.
--
-- ⚠️ ORDEN DE DEPLOY: crear los nombres NUEVOS y desplegar el jar que los
-- invoca ANTES de dropear los viejos (un jar viejo llamando al nombre viejo
-- falla si ya no existe). Este script dropea los viejos al final; correrlo
-- junto con el restart del servicio. En entornos con el jar viejo aún activo,
-- aplicar solo la parte CREATE y dropear los viejos tras el deploy.
-- ============================================================

DROP PROCEDURE IF EXISTS sp_importar_upsert;
DROP PROCEDURE IF EXISTS sp_importar_actualizar;

DELIMITER //

CREATE PROCEDURE sp_importar_upsert(
  IN  p_staging  VARCHAR(128),
  IN  p_dest     VARCHAR(128),
  IN  p_cols     TEXT,
  IN  p_set      TEXT,
  OUT p_inserted INT,
  OUT p_updated  INT
)
BEGIN
  DECLARE v_old_su INT;
  SET v_old_su = @@SQL_SAFE_UPDATES;
  SET SQL_SAFE_UPDATES = 0;

  SET @sql_u = CONCAT('UPDATE `', p_dest, '` d JOIN `', p_staging,
                      '` s ON d.id = s.__existing_id SET ', p_set,
                      ' WHERE s.__existing_id IS NOT NULL');
  PREPARE st FROM @sql_u; EXECUTE st; SET p_updated = ROW_COUNT(); DEALLOCATE PREPARE st;

  SET @sql_i = CONCAT('INSERT INTO `', p_dest, '` (', p_cols, ') SELECT ', p_cols,
                      ' FROM `', p_staging, '` WHERE __existing_id IS NULL');
  PREPARE st FROM @sql_i; EXECUTE st; SET p_inserted = ROW_COUNT(); DEALLOCATE PREPARE st;

  SET SQL_SAFE_UPDATES = v_old_su;
END //

CREATE PROCEDURE sp_importar_actualizar(
  IN  p_staging   VARCHAR(128),
  IN  p_dest      VARCHAR(128),
  IN  p_link_col  VARCHAR(128),
  IN  p_set       TEXT,
  OUT p_updated   INT,
  OUT p_not_found INT
)
BEGIN
  DECLARE v_old_su INT;
  SET v_old_su = @@SQL_SAFE_UPDATES;
  SET SQL_SAFE_UPDATES = 0;

  SET @sql_u = CONCAT('UPDATE `', p_dest, '` d JOIN `', p_staging,
                      '` s ON d.`', p_link_col, '` = s.__link SET ', p_set);
  PREPARE st FROM @sql_u; EXECUTE st; SET p_updated = ROW_COUNT(); DEALLOCATE PREPARE st;

  SET @sql_n = CONCAT('SELECT COUNT(*) INTO @nf FROM `', p_staging, '` s LEFT JOIN `', p_dest,
                      '` d ON d.`', p_link_col, '` = s.__link WHERE d.id IS NULL');
  PREPARE st FROM @sql_n; EXECUTE st; SET p_not_found = @nf; DEALLOCATE PREPARE st;

  SET SQL_SAFE_UPDATES = v_old_su;
END //

DELIMITER ;

-- Limpieza de los nombres viejos (correr junto al deploy del jar nuevo)
DROP PROCEDURE IF EXISTS sp_import_upsert;
DROP PROCEDURE IF EXISTS sp_import_update;
DROP PROCEDURE IF EXISTS sp_sync_customers;
