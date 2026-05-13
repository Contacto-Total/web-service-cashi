-- ========================================
-- MIGRACIÓN V16: SP sp_osiptel_archive
-- Archiva filas finalizadas de osiptel_validation_log con
-- finished_at < NOW() - INTERVAL p_retention_days DAY.
--
-- Uso: CALL sp_osiptel_archive(180, @archived, @msg);
-- Ejecutar mensualmente via @Scheduled o evento MySQL.
-- ========================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_osiptel_archive //

CREATE PROCEDURE sp_osiptel_archive(
    IN p_retention_days INT,
    OUT p_records_archived BIGINT,
    OUT p_message VARCHAR(500)
)
BEGIN
    DECLARE v_cutoff DATETIME;
    DECLARE v_count_before BIGINT DEFAULT 0;
    DECLARE v_count_after BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 p_message = MESSAGE_TEXT;
        SET p_records_archived = 0;
        ROLLBACK;
    END;

    IF p_retention_days IS NULL OR p_retention_days < 30 THEN
        SET p_records_archived = 0;
        SET p_message = 'p_retention_days invalido (minimo 30 dias)';
    ELSE
        SET v_cutoff = DATE_SUB(NOW(), INTERVAL p_retention_days DAY);

        START TRANSACTION;

        SELECT COUNT(*) INTO v_count_before
        FROM osiptel_validation_log
        WHERE status IN ('OK', 'NOT_FOUND', 'FAILED', 'EXPIRED')
          AND finished_at IS NOT NULL
          AND finished_at < v_cutoff;

        -- Mover a archive (osiptel_phone_match y osiptel_validation_attempt caen
        -- por ON DELETE CASCADE de la FK)
        INSERT INTO osiptel_validation_archive (
            id, dni_hash, dni_type, lines_count, status, attempts,
            enqueued_at, started_at, finished_at,
            source_customer_id, source_subportfolio_id,
            worker_id, fecha_creacion, fecha_archivado
        )
        SELECT
            id, dni_hash, dni_type, lines_count, status, attempts,
            enqueued_at, started_at, finished_at,
            source_customer_id, source_subportfolio_id,
            worker_id, fecha_creacion, NOW()
        FROM osiptel_validation_log
        WHERE status IN ('OK', 'NOT_FOUND', 'FAILED', 'EXPIRED')
          AND finished_at IS NOT NULL
          AND finished_at < v_cutoff;

        DELETE FROM osiptel_validation_log
        WHERE status IN ('OK', 'NOT_FOUND', 'FAILED', 'EXPIRED')
          AND finished_at IS NOT NULL
          AND finished_at < v_cutoff;

        SET v_count_after = ROW_COUNT();

        COMMIT;

        SET p_records_archived = v_count_after;
        SET p_message = CONCAT('Archivados: ', v_count_after,
                               ' (candidatos: ', v_count_before,
                               ', corte: ', v_cutoff, ')');
    END IF;
END //

DELIMITER ;
