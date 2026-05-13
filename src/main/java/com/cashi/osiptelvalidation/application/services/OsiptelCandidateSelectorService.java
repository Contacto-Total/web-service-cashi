package com.cashi.osiptelvalidation.application.services;

import com.cashi.osiptelvalidation.application.internal.commandservices.DniHashService;
import com.cashi.osiptelvalidation.domain.model.commands.EnqueueOsiptelBatchCommand;
import com.cashi.osiptelvalidation.domain.services.OsiptelCandidateSelector;
import com.cashi.osiptelvalidation.domain.services.OsiptelValidationCommandService;
import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cron nocturno (23:00 hora Lima) que selecciona candidatos de cashi_db.metodos_contacto
 * y los encola para validación Osiptel.
 *
 * Filtros aplicados:
 *  - tipo_contacto = 'telefono'
 *  - valor móvil PE: REGEXP '^9[0-9]{8}$'
 *  - clientes.documento DNI peruano: REGEXP '^[0-9]{8}$'
 *  - NO existe ya validación activa (PENDING/IN_PROGRESS), ni con cooldown vigente.
 *  - Prioriza subportfolios activos y dias_mora DESC.
 *
 * Implementa también OsiptelCandidateSelector para que pueda invocarse manualmente
 * desde un endpoint admin si se necesita.
 */
@Service
public class OsiptelCandidateSelectorService implements OsiptelCandidateSelector {

    private static final Logger log = LoggerFactory.getLogger(OsiptelCandidateSelectorService.class);

    private final JdbcTemplate jdbcTemplate;
    private final OsiptelValidationCommandService commandService;
    private final OsiptelProperties properties;

    @Autowired
    public OsiptelCandidateSelectorService(JdbcTemplate jdbcTemplate,
                                           OsiptelValidationCommandService commandService,
                                           OsiptelProperties properties,
                                           DniHashService unused) {
        this.jdbcTemplate = jdbcTemplate;
        this.commandService = commandService;
        this.properties = properties;
    }

    /**
     * Cron diario a las 23:00 hora Lima.
     * Si el feature flag está apagado, no corre.
     */
    @Scheduled(cron = "0 0 23 * * *", zone = "America/Lima")
    public void runNightlySelection() {
        if (!properties.isCandidateCronEnabled() || !properties.isLegalReviewSignedOff()) {
            log.info("Osiptel candidate cron deshabilitado (cronEnabled={}, legalSignedOff={})",
                    properties.isCandidateCronEnabled(), properties.isLegalReviewSignedOff());
            return;
        }
        int quota = properties.getDailyQuota();
        EnqueueOsiptelBatchCommand cmd = selectCandidates(quota);
        if (cmd.entries().isEmpty()) {
            log.info("Osiptel candidate cron: sin candidatos elegibles");
            return;
        }
        OsiptelValidationCommandService.EnqueueResult result = commandService.enqueueBatch(cmd);
        log.info("Osiptel candidate cron: {} encolados, {} saltados (batchId={})",
                result.enqueued(), result.skipped(), result.batchId());
    }

    @Override
    public EnqueueOsiptelBatchCommand selectCandidates(int dailyQuota) {
        String sql = """
                SELECT mc.id AS contact_method_id,
                       mc.valor AS phone,
                       c.documento AS dni,
                       c.id_inquilino AS tenant_id,
                       c.id_subcartera AS subportfolio_id
                FROM metodos_contacto mc
                INNER JOIN clientes c ON c.id = mc.id_cliente
                LEFT JOIN subcarteras sc ON sc.id = c.id_subcartera
                WHERE mc.tipo_contacto = 'telefono'
                  AND mc.valor REGEXP '^9[0-9]{8}$'
                  AND c.documento REGEXP '^[0-9]{8}$'
                  AND NOT EXISTS (
                      SELECT 1 FROM osiptel_validation_log o
                      WHERE o.phone = mc.valor
                        AND (
                            o.status IN ('PENDING', 'IN_PROGRESS')
                            OR (o.status IN ('OK', 'NOT_FOUND', 'FAILED')
                                AND o.cooldown_until IS NOT NULL
                                AND o.cooldown_until > NOW())
                            OR o.status = 'EXPIRED'
                        )
                  )
                ORDER BY COALESCE(c.dias_mora, 0) DESC, mc.id ASC
                LIMIT ?
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, dailyQuota);

        List<EnqueueOsiptelBatchCommand.PhoneEntry> entries = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            entries.add(new EnqueueOsiptelBatchCommand.PhoneEntry(
                    (String) row.get("phone"),
                    (String) row.get("dni"),
                    asLong(row.get("subportfolio_id")),
                    asLong(row.get("contact_method_id")),
                    asLong(row.get("tenant_id"))
            ));
        }
        log.info("Osiptel candidate selector: {} candidatos seleccionados (quota={})", entries.size(), dailyQuota);
        return new EnqueueOsiptelBatchCommand(entries);
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
    }
}
