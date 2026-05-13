package com.cashi.osiptelvalidation.application.services;

import com.cashi.osiptelvalidation.application.internal.commandservices.DniHashService;
import com.cashi.osiptelvalidation.domain.model.commands.EnqueueOsiptelBatchCommand;
import com.cashi.osiptelvalidation.domain.model.valueobjects.DocumentType;
import com.cashi.osiptelvalidation.domain.services.OsiptelCandidateSelector;
import com.cashi.osiptelvalidation.domain.services.OsiptelValidationCommandService;
import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cron nocturno: selecciona DNIs únicos de clientes que tienen al menos un
 * móvil PE y no tienen validación Osiptel activa / no en cooldown.
 *
 * Post-pivot: una consulta = un DNI (no un teléfono). Si un cliente tiene N
 * teléfonos, basta 1 validación para evaluarlos todos.
 *
 * NOTA sobre el hash en SQL: usamos SHA2(CONCAT(documento, ':', salt), 256)
 * en el NOT EXISTS para evitar tener que materializar todos los hashes en
 * memoria de Java. Esto requiere que la sal sea global (no por tenant).
 */
@Service
public class OsiptelCandidateSelectorService implements OsiptelCandidateSelector {

    private static final Logger log = LoggerFactory.getLogger(OsiptelCandidateSelectorService.class);

    private final JdbcTemplate jdbcTemplate;
    private final OsiptelValidationCommandService commandService;
    private final OsiptelProperties properties;
    @SuppressWarnings("unused")
    private final DniHashService dniHashService;  // se conserva la dependencia para coherencia

    public OsiptelCandidateSelectorService(JdbcTemplate jdbcTemplate,
                                           OsiptelValidationCommandService commandService,
                                           OsiptelProperties properties,
                                           DniHashService dniHashService) {
        this.jdbcTemplate = jdbcTemplate;
        this.commandService = commandService;
        this.properties = properties;
        this.dniHashService = dniHashService;
    }

    @Scheduled(cron = "0 0 23 * * *", zone = "America/Lima")
    public void runNightlySelection() {
        if (!properties.isCandidateCronEnabled() || !properties.isLegalReviewSignedOff()) {
            log.info("Osiptel candidate cron deshabilitado (cron={}, legal={})",
                    properties.isCandidateCronEnabled(), properties.isLegalReviewSignedOff());
            return;
        }
        EnqueueOsiptelBatchCommand cmd = selectCandidates(properties.getDailyQuota());
        if (cmd.entries().isEmpty()) {
            log.info("Osiptel candidate cron: sin candidatos elegibles");
            return;
        }
        OsiptelValidationCommandService.EnqueueResult res = commandService.enqueueBatch(cmd);
        log.info("Osiptel candidate cron: {} encolados, {} saltados (batchId={})",
                res.enqueued(), res.skipped(), res.batchId());
    }

    @Override
    public EnqueueOsiptelBatchCommand selectCandidates(int dailyQuota) {
        String sql = """
                SELECT
                    c.id AS customer_id,
                    c.documento AS dni,
                    c.id_inquilino AS tenant_id,
                    c.id_subcartera AS subportfolio_id
                FROM clientes c
                WHERE c.documento REGEXP '^[0-9]{8}$'
                  AND EXISTS (
                      SELECT 1 FROM metodos_contacto mc
                      WHERE mc.id_cliente = c.id
                        AND mc.tipo_contacto = 'telefono'
                        AND mc.valor REGEXP '^9[0-9]{8}$'
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM osiptel_validation_log o
                      WHERE o.dni_hash = SHA2(CONCAT(c.documento, ':', ?), 256)
                        AND (
                            o.status IN ('PENDING', 'IN_PROGRESS')
                            OR (o.status IN ('OK', 'NOT_FOUND', 'FAILED')
                                AND o.cooldown_until IS NOT NULL
                                AND o.cooldown_until > NOW())
                            OR o.status = 'EXPIRED'
                        )
                  )
                ORDER BY COALESCE(c.dias_mora, 0) DESC, c.id ASC
                LIMIT ?
                """;

        String salt = properties.getDniHashSalt();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, salt, dailyQuota);

        List<EnqueueOsiptelBatchCommand.DocumentEntry> entries = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            entries.add(new EnqueueOsiptelBatchCommand.DocumentEntry(
                    (String) row.get("dni"),
                    DocumentType.DNI,
                    asLong(row.get("customer_id")),
                    asLong(row.get("subportfolio_id")),
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
