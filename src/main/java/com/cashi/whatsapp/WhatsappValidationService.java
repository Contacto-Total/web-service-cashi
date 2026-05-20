package com.cashi.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio WhatsApp (modelo NO-ortogonal V17+).
 *
 * Pull model: el worker hace polling.
 *   getPendingQueue() → devuelve metodos_contacto con estado_whatsapp = SIN_VALIDAR
 *   applyResult()     → actualiza estado_whatsapp en la fila correspondiente
 *
 * Si el worker reporta ERROR, la fila NO se actualiza (queda SIN_VALIDAR para reintento).
 */
@Service
public class WhatsappValidationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappValidationService.class);

    private final JdbcTemplate jdbcTemplate;

    public WhatsappValidationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WhatsappController.QueueItem> getPendingQueue(int limit) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, valor AS phone " +
            "FROM cashi_db.metodos_contacto " +
            "WHERE tipo_contacto = 'telefono' AND estado_whatsapp = 'SIN_VALIDAR' " +
            "LIMIT ?", limit);

        return rows.stream()
            .map(r -> new WhatsappController.QueueItem(
                ((Number) r.get("id")).longValue(),
                (String) r.get("phone")))
            .collect(Collectors.toList());
    }

    public void applyResult(Long id, String status) {
        if ("VALIDADO".equals(status) || "NO_VALIDADO".equals(status)) {
            jdbcTemplate.update(
                "UPDATE cashi_db.metodos_contacto SET estado_whatsapp = ? WHERE id = ?",
                status, id);
            log.info("applyResult whatsapp: id={} -> {}", id, status);
        } else {
            log.warn("applyResult whatsapp: id={} status={} (sin update, queda SIN_VALIDAR para reintento)", id, status);
        }
    }
}
