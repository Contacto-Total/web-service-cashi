-- =====================================================
-- Stored Procedure: sp_limpiar_contactos_invalidos
-- Descripción: Reaplica en lote la regla operativa que inactiva teléfonos
--   tipificados como FALLECIDO o EQUIVOCADO en gestiones previas.
--   Pone metodos_contacto.estado = 'INACTIVE' para los teléfonos de la
--   subcartera que tengan alguna gestión con ruta_nivel_2 en (FALLECIDO, EQUIVOCADO).
-- Llamado desde: HeaderConfigurationCommandServiceImpl.reapplyInvalidPhoneContactInactivation
--   tras el sync de clientes en carga INICIAL e importDailyData (Fase 3).
-- NOTA: cruza dos schemas — cashi_db.metodos_contacto + cashi_discador_db.registros_gestion.
-- Uso: CALL sp_limpiar_contactos_invalidos(tenantId, carteraId, subcarteraId);
-- =====================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_limpiar_contactos_invalidos //

CREATE PROCEDURE sp_limpiar_contactos_invalidos(
    IN p_tenant_id INT,
    IN p_portfolio_id INT,
    IN p_sub_portfolio_id INT
)
BEGIN
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

    SET SQL_SAFE_UPDATES = 1;
END //

DELIMITER ;
