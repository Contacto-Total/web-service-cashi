-- ============================================================
-- V28 — sp_archivar_periodo: nombrar el archivo por el PERÍODO REAL de los datos.
--
-- Bug P1: PeriodSnapshotServiceImpl pasa `YearMonth.now()` como nombre del
-- archivo. Cuando el snapshot mensual corre el día 1 del mes siguiente, la tabla
-- histórica queda nombrada con +1 mes respecto al período de los datos
-- (p.ej. los datos de MAYO se archivaban como `foh_cas_cst_2026_06`).
--
-- Fix solo-BD (sin tocar el código Java): el SP IGNORA el parámetro recibido y
-- deriva el período de la columna `periodo` (YYYYMM) de la tabla origen:
--   MAX(periodo) -> 'YYYY_MM'. Si la tabla no tiene columna `periodo` o el valor
--   no es YYYYMM, usa el parámetro recibido (fallback).
--
-- Mantiene la idempotencia (DROP+CREATE del histórico) del fix P2 anterior.
-- NO afecta el snapshot diario (usa sp_archivar_diario, otro SP).
--
-- Aplicado manualmente en QAS y PROD el 2026-06-12 (Flyway inactivo). Esta
-- migración lo versiona para que un redeploy no revierta el SP a la versión
-- sin-derivación.
-- ============================================================

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
    DECLARE v_period VARCHAR(20);
    DECLARE v_has_periodo INT DEFAULT 0;
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

    -- ---- Derivar período del DATO (no de now()) ----
    SET v_period = p_archive_period;  -- fallback por defecto
    SELECT COUNT(*) INTO v_has_periodo FROM information_schema.columns
      WHERE table_schema = v_main_db AND table_name = p_table_name AND column_name = 'periodo';
    IF v_has_periodo > 0 THEN
        SET @maxp = NULL;
        SET @sql = CONCAT('SELECT MAX(`periodo`) INTO @maxp FROM `', v_main_db, '`.`', p_table_name, '` WHERE `periodo` IS NOT NULL');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        IF @maxp IS NOT NULL AND @maxp REGEXP '^[0-9]{6}$' THEN
            SET v_period = CONCAT(LEFT(@maxp, 4), '_', MID(@maxp, 5, 2));
        END IF;
    END IF;

    SET v_archived_table = CONCAT(p_table_name, '_', v_period);

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
