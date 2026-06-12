-- ============================================================================
-- deploy_prod_sp.sql — Despliegue de SPs de la Carga Consolidada en PROD.
--
-- Estado actual de PROD (3.141.174.202 / cashi_db):
--   - sp_import_upsert / sp_import_update : presentes (nombre VIEJO, con fix V25)
--   - sp_archivar_periodo                : presente PERO sin el fix P2 (NO idempotente)
--   - sp_sincronizar_clientes            : NO existe
--   - sp_importar_upsert/actualizar      : NO existen
--
-- Este script PARTE A es SEGURO de correr con el jar viejo aún activo: solo
-- crea/reemplaza SPs de forma aditiva y NO dropea sp_import_upsert/update
-- (que el jar viejo todavía invoca).
--
-- ORDEN DE DESPLIEGUE EN PROD:
--   1) Correr PARTE A (este script hasta la marca "FIN PARTE A").
--   2) Desplegar el jar nuevo (git pull qas + mvn clean package + restart).
--   3) Verificar arranque OK y una carga de prueba (log "[SP] ...").
--   4) Correr PARTE B (drops de los nombres viejos) — al final de este archivo.
--
--   mysql -h 3.141.174.202 -u admin -p cashi_db < deploy_prod_sp.sql
-- ============================================================================

-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║ PARTE A — pre-deploy (aditivo, seguro con el jar viejo activo)            ║
-- ╚══════════════════════════════════════════════════════════════════════════╝

-- ── A.1) sp_archivar_periodo IDEMPOTENTE (fix P2: DROP+CREATE del histórico) ──
DROP PROCEDURE IF EXISTS sp_archivar_periodo;
DELIMITER //
CREATE PROCEDURE sp_archivar_periodo(
    IN  p_table_name    VARCHAR(100),
    IN  p_archive_period VARCHAR(20),
    OUT p_records_archived BIGINT,
    OUT p_success       BOOLEAN,
    OUT p_message       VARCHAR(500)
)
BEGIN
    DECLARE v_main_db VARCHAR(50) DEFAULT 'cashi_db';
    DECLARE v_hist_db VARCHAR(50) DEFAULT 'cashi_historico_db';
    DECLARE v_archived_table VARCHAR(150);
    DECLARE v_records_before BIGINT DEFAULT 0;
    DECLARE v_records_after BIGINT DEFAULT 0;
    DECLARE v_table_exists INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 p_message = MESSAGE_TEXT;
        SET p_success = FALSE;
        SET p_records_archived = 0;
        ROLLBACK;
    END;
    SET v_archived_table = CONCAT(p_table_name, '_', p_archive_period);
    SELECT COUNT(*) INTO v_table_exists FROM information_schema.tables
      WHERE table_schema = v_main_db AND table_name = p_table_name;
    IF v_table_exists = 0 THEN
        SET p_success = FALSE; SET p_records_archived = 0;
        SET p_message = CONCAT('Tabla origen no existe: ', v_main_db, '.', p_table_name);
    ELSE
        START TRANSACTION;
        SET @sql = CONCAT('CREATE DATABASE IF NOT EXISTS ', v_hist_db);
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        SET @sql = CONCAT('SELECT COUNT(*) INTO @cnt FROM ', v_main_db, '.', p_table_name);
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        SET v_records_before = @cnt;
        IF v_records_before = 0 THEN
            SET p_success = TRUE; SET p_records_archived = 0;
            SET p_message = 'Tabla vacia, nada que archivar'; COMMIT;
        ELSE
            -- FIX P2: DROP + CREATE (idempotente) en vez de CREATE IF NOT EXISTS
            SET @sql = CONCAT('DROP TABLE IF EXISTS ', v_hist_db, '.', v_archived_table);
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
            SET @sql = CONCAT('CREATE TABLE ', v_hist_db, '.', v_archived_table, ' LIKE ', v_main_db, '.', p_table_name);
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
            SET @sql = CONCAT('INSERT INTO ', v_hist_db, '.', v_archived_table, ' SELECT * FROM ', v_main_db, '.', p_table_name);
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
            SET @sql = CONCAT('SELECT COUNT(*) INTO @cnt FROM ', v_hist_db, '.', v_archived_table);
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
            SET v_records_after = @cnt;
            IF v_records_after < v_records_before THEN
                SET p_success = FALSE;
                SET p_message = CONCAT('Error de integridad: esperados ', v_records_before, ', archivados ', v_records_after);
                SET p_records_archived = 0; ROLLBACK;
            ELSE
                SET @sql = CONCAT('TRUNCATE TABLE ', v_main_db, '.', p_table_name);
                PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
                BEGIN
                    DECLARE CONTINUE HANDLER FOR 1146 BEGIN END;
                    INSERT INTO notificaciones_sistema (tipo, titulo, mensaje, created_at)
                    VALUES ('ARCHIVADO_TABLA', CONCAT('Snapshot: ', p_table_name),
                        CONCAT(v_records_after, ' registros archivados a ', v_hist_db, '.', v_archived_table), NOW());
                END;
                SET p_success = TRUE; SET p_records_archived = v_records_after;
                SET p_message = CONCAT('OK: ', v_records_after, ' registros archivados a ', v_hist_db, '.', v_archived_table);
                COMMIT;
            END IF;
        END IF;
    END IF;
END //
DELIMITER ;

-- ── A.2) sp_importar_upsert + sp_importar_actualizar (nombres nuevos, = V25) ──
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

-- ── A.3) sp_sincronizar_clientes (sync set-based; = V26) ──
DROP PROCEDURE IF EXISTS sp_sincronizar_clientes;
DELIMITER //
CREATE PROCEDURE sp_sincronizar_clientes(
    IN  p_stg               VARCHAR(128),
    IN  p_tenant_id         BIGINT,
    IN  p_tenant_name       VARCHAR(255),
    IN  p_portfolio_id      BIGINT,
    IN  p_portfolio_name    VARCHAR(255),
    IN  p_subportfolio_id   BIGINT,
    IN  p_subportfolio_name VARCHAR(255),
    OUT p_cust_created      INT,
    OUT p_cust_updated      INT,
    OUT p_contacts_inserted INT
)
BEGIN
    DECLARE v_old_su INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        DROP TEMPORARY TABLE IF EXISTS _sync_clients;
        DROP TEMPORARY TABLE IF EXISTS _sync_new;
        DROP TEMPORARY TABLE IF EXISTS _sync_states;
        DROP TEMPORARY TABLE IF EXISTS _sync_manual;
        SET SQL_SAFE_UPDATES = v_old_su;
        RESIGNAL;
    END;

    SET v_old_su = @@SQL_SAFE_UPDATES;
    SET SQL_SAFE_UPDATES = 0;

    DROP TEMPORARY TABLE IF EXISTS _sync_clients;
    DROP TEMPORARY TABLE IF EXISTS _sync_new;
    DROP TEMPORARY TABLE IF EXISTS _sync_states;
    DROP TEMPORARY TABLE IF EXISTS _sync_manual;

    SET @sql = CONCAT(
        'SELECT ',
        ' SUM(CASE WHEN c.id IS NOT NULL THEN 1 ELSE 0 END), ',
        ' SUM(CASE WHEN c.id IS NULL THEN 1 ELSE 0 END) ',
        'INTO @upd, @cre ',
        'FROM `', p_stg, '` s ',
        'LEFT JOIN clientes c ON c.codigo_identificacion = s.codigo_identificacion ',
        'WHERE s.documento IS NOT NULL AND s.documento <> ''''');
    PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
    SET p_cust_updated = IFNULL(@upd, 0);
    SET p_cust_created = IFNULL(@cre, 0);

    SET @sql = CONCAT(
        'INSERT INTO clientes (',
        ' id_inquilino, nombre_inquilino, id_cartera, nombre_cartera, id_subcartera, nombre_subcartera,',
        ' id_cliente, codigo_identificacion, documento,',
        ' nombre_completo, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido,',
        ' fecha_nacimiento, edad, estado_civil, ocupacion, tipo_cliente,',
        ' direccion, distrito, provincia, departamento, referencia_personal,',
        ' numero_cuenta_linea_prestamo, dias_mora, monto_mora, monto_capital,',
        ' fecha_creacion, fecha_actualizacion) ',
        'SELECT ',
        ' ', p_tenant_id, ', ', QUOTE(p_tenant_name), ', ', p_portfolio_id, ', ', QUOTE(p_portfolio_name), ', ',
              p_subportfolio_id, ', ', QUOTE(p_subportfolio_name), ',',
        ' s.id_cliente, s.codigo_identificacion, s.documento,',
        ' s.nombre_completo, s.primer_nombre, s.segundo_nombre, s.primer_apellido, s.segundo_apellido,',
        ' s.fecha_nacimiento, s.edad, s.estado_civil, s.ocupacion, s.tipo_cliente,',
        ' s.direccion, s.distrito, s.provincia, s.departamento, s.referencia_personal,',
        ' s.numero_cuenta_linea_prestamo, s.dias_mora, s.monto_mora, s.monto_capital,',
        ' NOW(), NOW() ',
        'FROM `', p_stg, '` s ',
        'WHERE s.documento IS NOT NULL AND s.documento <> '''' ',
        'ON DUPLICATE KEY UPDATE ',
        ' id_inquilino=VALUES(id_inquilino), nombre_inquilino=VALUES(nombre_inquilino),',
        ' id_cartera=VALUES(id_cartera), nombre_cartera=VALUES(nombre_cartera),',
        ' id_subcartera=VALUES(id_subcartera), nombre_subcartera=VALUES(nombre_subcartera),',
        ' nombre_completo=VALUES(nombre_completo), primer_nombre=VALUES(primer_nombre),',
        ' segundo_nombre=VALUES(segundo_nombre), primer_apellido=VALUES(primer_apellido),',
        ' segundo_apellido=VALUES(segundo_apellido), fecha_nacimiento=VALUES(fecha_nacimiento),',
        ' edad=VALUES(edad), estado_civil=VALUES(estado_civil), ocupacion=VALUES(ocupacion),',
        ' tipo_cliente=VALUES(tipo_cliente), direccion=VALUES(direccion), distrito=VALUES(distrito),',
        ' provincia=VALUES(provincia), departamento=VALUES(departamento),',
        ' referencia_personal=VALUES(referencia_personal),',
        ' numero_cuenta_linea_prestamo=VALUES(numero_cuenta_linea_prestamo),',
        ' dias_mora=VALUES(dias_mora), monto_mora=VALUES(monto_mora), monto_capital=VALUES(monto_capital),',
        ' fecha_actualizacion=NOW()');
    PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

    CREATE TEMPORARY TABLE _sync_clients (id_cliente BIGINT PRIMARY KEY) ENGINE=InnoDB;
    SET @sql = CONCAT(
        'INSERT INTO _sync_clients (id_cliente) ',
        'SELECT DISTINCT c.id FROM `', p_stg, '` s ',
        'JOIN clientes c ON c.codigo_identificacion = s.codigo_identificacion ',
        'WHERE s.documento IS NOT NULL AND s.documento <> ''''');
    PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

    CREATE TEMPORARY TABLE _sync_new (
        id_cliente BIGINT, tipo_contacto VARCHAR(20), subtipo VARCHAR(40),
        valor VARCHAR(255), nval VARCHAR(255),
        KEY k (id_cliente, tipo_contacto, nval)
    ) ENGINE=InnoDB;
    SET @sql = CONCAT(
        'INSERT INTO _sync_new (id_cliente, tipo_contacto, subtipo, valor, nval) ',
        'SELECT z.id_cliente, z.tc, z.st, TRIM(z.val), LOWER(TRIM(z.val)) FROM ( ',
        '  SELECT c.id AS id_cliente, sel.tc, sel.st, ',
        '    CASE sel.st ',
        '      WHEN ''telefono_principal''   THEN s.telefono_principal ',
        '      WHEN ''telefono_secundario''  THEN s.telefono_secundario ',
        '      WHEN ''telefono_trabajo''     THEN s.telefono_trabajo ',
        '      WHEN ''telefono_referencia_1''THEN s.telefono_referencia_1 ',
        '      WHEN ''telefono_referencia_2''THEN s.telefono_referencia_2 ',
        '      WHEN ''email''                THEN s.email ',
        '    END AS val ',
        '  FROM `', p_stg, '` s ',
        '  JOIN clientes c ON c.codigo_identificacion = s.codigo_identificacion ',
        '  CROSS JOIN ( ',
        '             SELECT ''telefono'' tc, ''telefono_principal''   st ',
        '   UNION ALL SELECT ''telefono'',    ''telefono_secundario'' ',
        '   UNION ALL SELECT ''telefono'',    ''telefono_trabajo'' ',
        '   UNION ALL SELECT ''telefono'',    ''telefono_referencia_1'' ',
        '   UNION ALL SELECT ''telefono'',    ''telefono_referencia_2'' ',
        '   UNION ALL SELECT ''email'',       ''email'' ',
        '  ) sel ',
        '  WHERE s.documento IS NOT NULL AND s.documento <> '''' ',
        ') z ',
        'WHERE z.val IS NOT NULL AND TRIM(z.val) <> ''''');
    PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

    CREATE TEMPORARY TABLE _sync_states (
        id_cliente BIGINT, tipo_contacto VARCHAR(20), nval VARCHAR(255),
        estado VARCHAR(20), estado_osiptel VARCHAR(40), estado_whatsapp VARCHAR(40),
        estado_contactabilidad VARCHAR(40), fecha_importacion DATE,
        KEY k (id_cliente, tipo_contacto, nval)
    ) ENGINE=InnoDB;
    INSERT INTO _sync_states
    SELECT t.id_cliente, t.tipo_contacto, LOWER(TRIM(t.valor)),
           t.estado, t.estado_osiptel, t.estado_whatsapp, t.estado_contactabilidad, t.fecha_importacion
    FROM metodos_contacto t
    JOIN ( SELECT id_cliente, tipo_contacto, LOWER(TRIM(valor)) nval, MIN(id) minid
           FROM metodos_contacto
           WHERE id_cliente IN (SELECT id_cliente FROM _sync_clients)
             AND subtipo IN ('telefono_principal','telefono_secundario','telefono_trabajo',
                             'email','telefono_referencia_1','telefono_referencia_2')
           GROUP BY id_cliente, tipo_contacto, LOWER(TRIM(valor)) ) k
      ON k.minid = t.id;

    CREATE TEMPORARY TABLE _sync_manual (
        id_cliente BIGINT, tipo_contacto VARCHAR(20), nval VARCHAR(255),
        KEY k (id_cliente, tipo_contacto, nval)
    ) ENGINE=InnoDB;
    INSERT INTO _sync_manual
    SELECT DISTINCT id_cliente, tipo_contacto, LOWER(TRIM(valor))
    FROM metodos_contacto
    WHERE id_cliente IN (SELECT id_cliente FROM _sync_clients)
      AND LOWER(TRIM(etiqueta)) = LOWER(TRIM('Agregado por agente'));

    DELETE m FROM metodos_contacto m
    JOIN _sync_clients sc ON sc.id_cliente = m.id_cliente
    WHERE m.subtipo IN ('telefono_principal','telefono_secundario','telefono_trabajo',
                        'email','telefono_referencia_1','telefono_referencia_2')
      AND (m.etiqueta IS NULL OR LOWER(TRIM(m.etiqueta)) <> LOWER(TRIM('Agregado por agente')));

    INSERT INTO metodos_contacto
        (id_cliente, tipo_contacto, subtipo, valor, etiqueta, fecha_importacion,
         estado, estado_osiptel, estado_whatsapp, estado_contactabilidad)
    SELECT n.id_cliente, n.tipo_contacto, n.subtipo, n.valor, n.subtipo,
           COALESCE(st.fecha_importacion, CURDATE()),
           COALESCE(st.estado, 'ACTIVE'),
           COALESCE(st.estado_osiptel, 'SIN_VALIDAR'),
           COALESCE(st.estado_whatsapp, 'SIN_VALIDAR'),
           COALESCE(st.estado_contactabilidad, 'NUEVO')
    FROM _sync_new n
    LEFT JOIN _sync_states st
           ON st.id_cliente = n.id_cliente AND st.tipo_contacto = n.tipo_contacto AND st.nval = n.nval
    LEFT JOIN _sync_manual mn
           ON mn.id_cliente = n.id_cliente AND mn.tipo_contacto = n.tipo_contacto AND mn.nval = n.nval
    WHERE mn.id_cliente IS NULL;
    SET p_contacts_inserted = ROW_COUNT();

    DROP TEMPORARY TABLE IF EXISTS _sync_clients;
    DROP TEMPORARY TABLE IF EXISTS _sync_new;
    DROP TEMPORARY TABLE IF EXISTS _sync_states;
    DROP TEMPORARY TABLE IF EXISTS _sync_manual;

    SET SQL_SAFE_UPDATES = v_old_su;
END //
DELIMITER ;

-- ════════════════════════════════ FIN PARTE A ══════════════════════════════
-- (Desplegar el jar nuevo y verificar ANTES de correr la PARTE B.)


-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║ PARTE B — post-deploy (correr SOLO tras confirmar el jar nuevo en prod)   ║
-- ║ Elimina los nombres viejos que ya nadie invoca.                           ║
-- ╚══════════════════════════════════════════════════════════════════════════╝
-- DROP PROCEDURE IF EXISTS sp_import_upsert;
-- DROP PROCEDURE IF EXISTS sp_import_update;
