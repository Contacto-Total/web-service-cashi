-- ============================================================================
-- V28 — Evitar TELÉFONOS/EMAILS DUPLICADOS por cliente en metodos_contacto.
--
-- Problema detectado:
--   sp_sincronizar_clientes (carga consolidada) "despivota" los 6 subtipos
--   gestionados (telefono_principal/secundario/trabajo/referencia_1/referencia_2/
--   email) e inserta UNA fila por subtipo SIN deduplicar por valor. Si el archivo
--   trae el MISMO número en 2+ columnas (p.ej. principal y referencia_1), se
--   insertan 2 filas con el mismo `valor` para el mismo cliente => DUPLICADO.
--   Tampoco existía un constraint que lo impidiera a nivel de BD.
--
-- IMPORTANTE (escenario válido que se PRESERVA):
--   El mismo número en CLIENTES DISTINTOS es legítimo (teléfonos fijos, centrales,
--   referencias compartidas: hay ~341 números compartidos por 2+ clientes, hasta
--   63 clientes en uno). Por eso la unicidad es POR CLIENTE
--   (id_cliente, tipo_contacto, valor), NUNCA global sobre `valor`.
--
-- Cambios:
--   1) Dedup defensivo de duplicados ya existentes (hoy son 0; no-op, pero deja
--      la tabla apta para el índice único aunque aparezcan antes del deploy).
--   2) Índice ÚNICO (id_cliente, tipo_contacto, valor) como barrera de BD.
--   3) sp_sincronizar_clientes: deduplica _sync_new por (id_cliente, tipo_contacto,
--      nval) quedándose con el subtipo de mayor prioridad
--      (principal>secundario>trabajo>ref1>ref2). El INSERT final usa INSERT IGNORE
--      como respaldo del índice único.
-- ============================================================================

-- 1) Dedup defensivo: conservar la fila de menor id por (cliente, tipo, valor exacto).
--    SQL_SAFE_UPDATES off/restore (el DELETE con JOIN puede chocar con safe-updates
--    en la conexión Flyway; mismo cuidado que V25).
SET @v_old_safe := @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

DELETE m FROM metodos_contacto m
JOIN (
    SELECT id_cliente, tipo_contacto, valor, MIN(id) AS keep_id
    FROM metodos_contacto
    GROUP BY id_cliente, tipo_contacto, valor
    HAVING COUNT(*) > 1
) d
  ON d.id_cliente = m.id_cliente
 AND d.tipo_contacto = m.tipo_contacto
 AND d.valor = m.valor
 AND m.id <> d.keep_id;

SET SQL_SAFE_UPDATES = @v_old_safe;

-- 2) Barrera de BD: único POR CLIENTE (permite el mismo número en clientes distintos).
ALTER TABLE metodos_contacto
    ADD UNIQUE INDEX uq_mc_cliente_tipo_valor (id_cliente, tipo_contacto, valor);

-- 3) SP con dedup por valor.
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
        DROP TEMPORARY TABLE IF EXISTS _sync_new_dedup;
        DROP TEMPORARY TABLE IF EXISTS _sync_states;
        DROP TEMPORARY TABLE IF EXISTS _sync_manual;
        SET SQL_SAFE_UPDATES = v_old_su;
        RESIGNAL;
    END;

    SET v_old_su = @@SQL_SAFE_UPDATES;
    SET SQL_SAFE_UPDATES = 0;

    DROP TEMPORARY TABLE IF EXISTS _sync_clients;
    DROP TEMPORARY TABLE IF EXISTS _sync_new;
    DROP TEMPORARY TABLE IF EXISTS _sync_new_dedup;
    DROP TEMPORARY TABLE IF EXISTS _sync_states;
    DROP TEMPORARY TABLE IF EXISTS _sync_manual;

    -- 0) Conteo created/updated ANTES del upsert
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

    -- 1) UPSERT a clientes
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

    -- 2) Clientes afectados
    CREATE TEMPORARY TABLE _sync_clients (id_cliente BIGINT PRIMARY KEY) ENGINE=InnoDB;
    SET @sql = CONCAT(
        'INSERT INTO _sync_clients (id_cliente) ',
        'SELECT DISTINCT c.id FROM `', p_stg, '` s ',
        'JOIN clientes c ON c.codigo_identificacion = s.codigo_identificacion ',
        'WHERE s.documento IS NOT NULL AND s.documento <> ''''');
    PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

    -- 3) Contactos NUEVOS del archivo (UNPIVOT de los 6 subtipos gestionados)
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

    -- 3b) DEDUP por valor: una sola fila por (id_cliente, tipo_contacto, nval),
    --     quedándose con el subtipo de mayor prioridad. INSERT IGNORE + ORDER BY
    --     => gana el primero en orden de prioridad.
    CREATE TEMPORARY TABLE _sync_new_dedup (
        id_cliente BIGINT, tipo_contacto VARCHAR(20), subtipo VARCHAR(40),
        valor VARCHAR(255), nval VARCHAR(255),
        UNIQUE KEY uq (id_cliente, tipo_contacto, nval)
    ) ENGINE=InnoDB;
    INSERT IGNORE INTO _sync_new_dedup (id_cliente, tipo_contacto, subtipo, valor, nval)
    SELECT id_cliente, tipo_contacto, subtipo, valor, nval
    FROM _sync_new
    ORDER BY id_cliente, tipo_contacto, nval,
        CASE subtipo
            WHEN 'telefono_principal'    THEN 1
            WHEN 'telefono_secundario'   THEN 2
            WHEN 'telefono_trabajo'      THEN 3
            WHEN 'telefono_referencia_1' THEN 4
            WHEN 'telefono_referencia_2' THEN 5
            ELSE 9
        END;

    -- 4) Snapshot de estados existentes (1 por id_cliente+tipo+nval, el de MIN(id))
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

    -- 5) Claves de contactos MANUALES (etiqueta='Agregado por agente')
    CREATE TEMPORARY TABLE _sync_manual (
        id_cliente BIGINT, tipo_contacto VARCHAR(20), nval VARCHAR(255),
        KEY k (id_cliente, tipo_contacto, nval)
    ) ENGINE=InnoDB;
    INSERT INTO _sync_manual
    SELECT DISTINCT id_cliente, tipo_contacto, LOWER(TRIM(valor))
    FROM metodos_contacto
    WHERE id_cliente IN (SELECT id_cliente FROM _sync_clients)
      AND LOWER(TRIM(etiqueta)) = LOWER(TRIM('Agregado por agente'));

    -- 6) DELETE de los subtipos gestionados (excepto los manuales)
    DELETE m FROM metodos_contacto m
    JOIN _sync_clients sc ON sc.id_cliente = m.id_cliente
    WHERE m.subtipo IN ('telefono_principal','telefono_secundario','telefono_trabajo',
                        'email','telefono_referencia_1','telefono_referencia_2')
      AND (m.etiqueta IS NULL OR LOWER(TRIM(m.etiqueta)) <> LOWER(TRIM('Agregado por agente')));

    -- 7) INSERT de los nuevos (ya deduplicados), preservando estado o defaults,
    --    excluyendo los que colisionan con un contacto manual existente.
    --    INSERT IGNORE: respaldo del índice único uq_mc_cliente_tipo_valor.
    INSERT IGNORE INTO metodos_contacto
        (id_cliente, tipo_contacto, subtipo, valor, etiqueta, fecha_importacion,
         estado, estado_osiptel, estado_whatsapp, estado_contactabilidad)
    SELECT n.id_cliente, n.tipo_contacto, n.subtipo, n.valor, n.subtipo,
           COALESCE(st.fecha_importacion, CURDATE()),
           COALESCE(st.estado, 'ACTIVE'),
           COALESCE(st.estado_osiptel, 'SIN_VALIDAR'),
           COALESCE(st.estado_whatsapp, 'SIN_VALIDAR'),
           COALESCE(st.estado_contactabilidad, 'NUEVO')
    FROM _sync_new_dedup n
    LEFT JOIN _sync_states st
           ON st.id_cliente = n.id_cliente AND st.tipo_contacto = n.tipo_contacto AND st.nval = n.nval
    LEFT JOIN _sync_manual mn
           ON mn.id_cliente = n.id_cliente AND mn.tipo_contacto = n.tipo_contacto AND mn.nval = n.nval
    WHERE mn.id_cliente IS NULL;
    SET p_contacts_inserted = ROW_COUNT();

    DROP TEMPORARY TABLE IF EXISTS _sync_clients;
    DROP TEMPORARY TABLE IF EXISTS _sync_new;
    DROP TEMPORARY TABLE IF EXISTS _sync_new_dedup;
    DROP TEMPORARY TABLE IF EXISTS _sync_states;
    DROP TEMPORARY TABLE IF EXISTS _sync_manual;

    SET SQL_SAFE_UPDATES = v_old_su;
END //

DELIMITER ;
