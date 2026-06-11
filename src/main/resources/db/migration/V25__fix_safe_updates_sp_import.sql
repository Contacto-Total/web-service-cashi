-- ============================================================
-- V25 — Fix SQL_SAFE_UPDATES en los SP de carga + raíz en sp_limpiar.
--
-- Bug detectado en validación e2e (motor sp): la diaria fallaba con
--   ERROR 1175 (safe update mode ... without a WHERE that uses a KEY column)
-- en sp_import_upsert. Causa raíz: sp_limpiar_contactos_invalidos hacía
-- "SET SQL_SAFE_UPDATES=1" al final (el default es 0) y dejaba la conexión
-- del pool "envenenada" en safe-update=1. La siguiente carga que reusaba esa
-- conexión chocaba, porque el UPDATE...JOIN del SP no lleva WHERE sobre una
-- columna KEY del destino (el legacy sí, usa WHERE id=?, por eso no lo sufría).
--
-- Fix:
--  1) sp_import_upsert / sp_import_update: fijan SQL_SAFE_UPDATES=0 al entrar y
--     RESTAURAN el valor previo al salir (robustos ante cualquier estado de conexión).
--  2) sp_limpiar_contactos_invalidos: guarda y RESTAURA el valor previo en vez de
--     forzar 1 (elimina el envenenamiento del pool en la raíz).
-- ============================================================

DROP PROCEDURE IF EXISTS sp_import_upsert;
DROP PROCEDURE IF EXISTS sp_import_update;
DROP PROCEDURE IF EXISTS sp_limpiar_contactos_invalidos;

DELIMITER //

CREATE PROCEDURE sp_import_upsert(
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

CREATE PROCEDURE sp_import_update(
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

CREATE PROCEDURE sp_limpiar_contactos_invalidos(
    IN p_tenant_id INT,
    IN p_portfolio_id INT,
    IN p_sub_portfolio_id INT
)
BEGIN
    DECLARE v_old_su INT;
    SET v_old_su = @@SQL_SAFE_UPDATES;
    SET SQL_SAFE_UPDATES = 0;
    UPDATE cashi_db.metodos_contacto m
    JOIN cashi_discador_db.registros_gestion g ON m.valor = g.telefono_contacto
    SET m.estado = 'INACTIVE'
    WHERE g.ruta_nivel_2 IN ('FALLECIDO', 'EQUIVOCADO')
      AND m.estado = 'ACTIVE'
      AND m.id_cliente = g.id_cliente
      AND g.id_tenant = p_tenant_id
      AND g.id_cartera = p_portfolio_id
      AND g.id_subcartera = p_sub_portfolio_id;
    SET SQL_SAFE_UPDATES = v_old_su;
END //

DELIMITER ;
