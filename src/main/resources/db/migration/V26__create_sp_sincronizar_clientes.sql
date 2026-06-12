-- ============================================================================
-- V26 — sp_sync_customers: sincronización de clientes + métodos_contacto SET-BASED.
--
-- Reemplaza el sync fila-por-fila de CustomerSyncService (que sufría un N+1:
-- mapColumnsToSystemFields consultaba configuracion_cabeceras por CADA fila ->
-- ~5 min para 4461 filas). El SP hace todo en operaciones de conjunto.
--
-- CONTRATO CON JAVA (patrón idéntico a sp_import_upsert):
--   Java resuelve el mapeo dinámico (fieldCode -> columna foh) UNA sola vez y
--   materializa una tabla TEMPORARY "staging canónica" con NOMBRES FIJOS:
--     codigo_identificacion, id_cliente, documento, nombre_completo, primer_nombre,
--     segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, edad,
--     estado_civil, ocupacion, tipo_cliente, direccion, distrito, provincia,
--     departamento, referencia_personal, numero_cuenta_linea_prestamo,
--     dias_mora, monto_mora, monto_capital,
--     telefono_principal, telefono_secundario, telefono_trabajo,
--     telefono_referencia_1, telefono_referencia_2, email
--   - Java aplica el fallback codigo_identificacion = COALESCE(NULLIF(identity,''), documento).
--   - Para sync SELECTIVO (diaria) Java filtra la staging por los identity codes.
--   El SP es TOTALMENTE ESTÁTICO sobre esos nombres (solo el nombre de la tabla
--   staging es dinámico, igual que en los SP de import). Tipos de columnas de la
--   staging = tipos de las columnas destino en `clientes` (coerción ya resuelta).
--
-- INVARIANTES PRESERVADAS (idénticas al Java legacy):
--   1) Salta filas con documento vacío ("Documento vacío en registro").
--   2) UPSERT clientes por UNIQUE codigo_identificacion; setea jerarquía completa;
--      id_cliente = documento.
--   3) Reconciliación de metodos_contacto SOLO sobre los 6 subtipos gestionados
--      (telefono_principal/secundario/trabajo/referencia_1/referencia_2/email).
--      NO toca otros subtipos (p.ej. telefono_extra).
--   4) PRESERVA estado/estado_osiptel/estado_whatsapp/estado_contactabilidad/
--      fecha_importacion de un contacto que reaparece (match por id_cliente +
--      tipo_contacto + LOWER(TRIM(valor))). Si es nuevo: ACTIVE/SIN_VALIDAR/
--      SIN_VALIDAR/NUEVO/CURDATE().
--   5) Contactos MANUALES (etiqueta='Agregado por agente') NO se borran y NO se
--      duplican (un contacto del archivo con mismo cliente/tipo/valor no se reinserta).
--   6) DELETE acotado: subtipos gestionados y etiqueta <> 'Agregado por agente'.
--
-- SQL_SAFE_UPDATES: save/restore (no envenenar el pool), igual que V25.
-- ============================================================================

DROP PROCEDURE IF EXISTS sp_sync_customers;

DELIMITER //

CREATE PROCEDURE sp_sync_customers(
    IN  p_stg               VARCHAR(128),   -- tabla TEMPORARY canónica (misma conexión)
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
        -- limpiar temporales y propagar el error a Java (rollback lo hace @Transactional)
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

    -- ───────────────────────────────────────────────────────────────────────
    -- 0) Conteo created/updated ANTES del upsert (para reportar igual que Java)
    -- ───────────────────────────────────────────────────────────────────────
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

    -- ───────────────────────────────────────────────────────────────────────
    -- 1) UPSERT a clientes (set-based, por UNIQUE codigo_identificacion)
    -- ───────────────────────────────────────────────────────────────────────
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

    -- ───────────────────────────────────────────────────────────────────────
    -- 2) Clientes afectados por este sync (los que tienen fila en staging)
    -- ───────────────────────────────────────────────────────────────────────
    CREATE TEMPORARY TABLE _sync_clients (id_cliente BIGINT PRIMARY KEY) ENGINE=InnoDB;
    SET @sql = CONCAT(
        'INSERT INTO _sync_clients (id_cliente) ',
        'SELECT DISTINCT c.id FROM `', p_stg, '` s ',
        'JOIN clientes c ON c.codigo_identificacion = s.codigo_identificacion ',
        'WHERE s.documento IS NOT NULL AND s.documento <> ''''');
    PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

    -- ───────────────────────────────────────────────────────────────────────
    -- 3) Contactos NUEVOS del archivo (UNPIVOT de los 6 subtipos gestionados).
    --    Solo valores no vacíos. nval = clave normalizada (lower+trim).
    --    OJO MySQL: una tabla TEMPORARY no puede referenciarse 2+ veces en la
    --    misma sentencia (error 1137). Por eso NO se usa UNION ALL sobre la
    --    staging; se referencia UNA sola vez y se "despivota" con un CROSS JOIN
    --    a un selector inline de 6 filas + CASE para elegir la columna.
    -- ───────────────────────────────────────────────────────────────────────
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

    -- ───────────────────────────────────────────────────────────────────────
    -- 4) Snapshot de estados existentes (1 por id_cliente+tipo+nval, el de MIN(id))
    --    para preservar validaciones tras el DELETE+INSERT.
    -- ───────────────────────────────────────────────────────────────────────
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

    -- ───────────────────────────────────────────────────────────────────────
    -- 5) Claves de contactos MANUALES (etiqueta='Agregado por agente') — sin
    --    filtro de subtipo (igual que loadPreservedManualContactKeys).
    -- ───────────────────────────────────────────────────────────────────────
    CREATE TEMPORARY TABLE _sync_manual (
        id_cliente BIGINT, tipo_contacto VARCHAR(20), nval VARCHAR(255),
        KEY k (id_cliente, tipo_contacto, nval)
    ) ENGINE=InnoDB;
    INSERT INTO _sync_manual
    SELECT DISTINCT id_cliente, tipo_contacto, LOWER(TRIM(valor))
    FROM metodos_contacto
    WHERE id_cliente IN (SELECT id_cliente FROM _sync_clients)
      AND LOWER(TRIM(etiqueta)) = LOWER(TRIM('Agregado por agente'));

    -- ───────────────────────────────────────────────────────────────────────
    -- 6) DELETE de los subtipos gestionados (excepto los manuales)
    -- ───────────────────────────────────────────────────────────────────────
    DELETE m FROM metodos_contacto m
    JOIN _sync_clients sc ON sc.id_cliente = m.id_cliente
    WHERE m.subtipo IN ('telefono_principal','telefono_secundario','telefono_trabajo',
                        'email','telefono_referencia_1','telefono_referencia_2')
      AND (m.etiqueta IS NULL OR LOWER(TRIM(m.etiqueta)) <> LOWER(TRIM('Agregado por agente')));

    -- ───────────────────────────────────────────────────────────────────────
    -- 7) INSERT de los nuevos, preservando estado (o defaults) y excluyendo los
    --    que colisionan con un contacto manual existente.
    -- ───────────────────────────────────────────────────────────────────────
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
