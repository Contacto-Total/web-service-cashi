-- =====================================================
-- Stored Procedure: sp_archivar_periodo
-- Descripción: Archiva una tabla de subcartera a la BD histórica
-- Uso: CALL sp_archivar_periodo('ini_sam_mas_elm', '2025_01', @records, @success, @msg);
-- =====================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_archivar_periodo //

CREATE PROCEDURE sp_archivar_periodo(
    IN p_table_name VARCHAR(100),
    IN p_archive_period VARCHAR(10),
    OUT p_records_archived BIGINT,
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(500)
)
BEGIN
    DECLARE v_main_db VARCHAR(50) DEFAULT 'cashi_db';
    DECLARE v_hist_db VARCHAR(50) DEFAULT 'cashi_historico_db';
    DECLARE v_archived_table VARCHAR(150);
    DECLARE v_records_before BIGINT DEFAULT 0;
    DECLARE v_records_after BIGINT DEFAULT 0;
    DECLARE v_table_exists INT DEFAULT 0;

    -- Handler para errores SQL
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 p_message = MESSAGE_TEXT;
        SET p_success = FALSE;
        SET p_records_archived = 0;
        ROLLBACK;
    END;

    -- Construir nombre de tabla archivada
    SET v_archived_table = CONCAT(p_table_name, '_', p_archive_period);

    -- Verificar que la tabla origen existe
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = v_main_db AND table_name = p_table_name;

    IF v_table_exists = 0 THEN
        SET p_success = FALSE;
        SET p_records_archived = 0;
        SET p_message = CONCAT('Tabla origen no existe: ', v_main_db, '.', p_table_name);
    ELSE
        START TRANSACTION;

        -- 1. Asegurar que BD histórica existe
        SET @sql = CONCAT('CREATE DATABASE IF NOT EXISTS ', v_hist_db);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        -- 2. Contar registros antes de archivar
        SET @sql = CONCAT('SELECT COUNT(*) INTO @cnt FROM ', v_main_db, '.', p_table_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SET v_records_before = @cnt;

        -- Si no hay registros, no hay nada que archivar
        IF v_records_before = 0 THEN
            SET p_success = TRUE;
            SET p_records_archived = 0;
            SET p_message = 'Tabla vacía, nada que archivar';
            COMMIT;
        ELSE
            -- 3. Crear tabla histórica con misma estructura
            SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS ', v_hist_db, '.', v_archived_table,
                              ' LIKE ', v_main_db, '.', p_table_name);
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            -- 4. Copiar todos los datos a tabla histórica
            SET @sql = CONCAT('INSERT INTO ', v_hist_db, '.', v_archived_table,
                              ' SELECT * FROM ', v_main_db, '.', p_table_name);
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            -- 5. Verificar que la copia fue exitosa
            SET @sql = CONCAT('SELECT COUNT(*) INTO @cnt FROM ', v_hist_db, '.', v_archived_table);
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SET v_records_after = @cnt;

            -- 6. Validar integridad antes de truncar
            IF v_records_after < v_records_before THEN
                SET p_success = FALSE;
                SET p_message = CONCAT('Error de integridad: esperados ', v_records_before,
                                      ', archivados ', v_records_after);
                SET p_records_archived = 0;
                ROLLBACK;
            ELSE
                -- 7. Truncar tabla original (más eficiente que DELETE)
                SET @sql = CONCAT('TRUNCATE TABLE ', v_main_db, '.', p_table_name);
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;

                -- 8. Registrar en notificaciones del sistema
                INSERT INTO notificaciones_sistema (tipo, titulo, mensaje, created_at)
                VALUES (
                    'ARCHIVADO_TABLA',
                    CONCAT('Snapshot: ', p_table_name),
                    CONCAT(v_records_after, ' registros archivados a ', v_hist_db, '.', v_archived_table),
                    NOW()
                );

                -- Resultado exitoso
                SET p_success = TRUE;
                SET p_records_archived = v_records_after;
                SET p_message = CONCAT('OK: ', v_records_after, ' registros archivados a ',
                                      v_hist_db, '.', v_archived_table);

                COMMIT;
            END IF;
        END IF;
    END IF;
END //

DELIMITER ;
