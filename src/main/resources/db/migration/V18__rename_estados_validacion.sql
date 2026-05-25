-- Renombra los valores de estado_osiptel y estado_whatsapp al nuevo vocabulario.
-- estado_osiptel: VALIDADO -> PERTENECE, NO_VALIDADO -> NO_PERTENECE
-- estado_whatsapp: VALIDADO -> TIENE,    NO_VALIDADO -> NO_TIENE
-- Los registros SIN_VALIDAR no cambian.

UPDATE cashi_db.metodos_contacto SET estado_osiptel = 'PERTENECE'    WHERE estado_osiptel = 'VALIDADO';
UPDATE cashi_db.metodos_contacto SET estado_osiptel = 'NO_PERTENECE' WHERE estado_osiptel = 'NO_VALIDADO';
UPDATE cashi_db.metodos_contacto SET estado_whatsapp = 'TIENE'       WHERE estado_whatsapp = 'VALIDADO';
UPDATE cashi_db.metodos_contacto SET estado_whatsapp = 'NO_TIENE'    WHERE estado_whatsapp = 'NO_VALIDADO';
