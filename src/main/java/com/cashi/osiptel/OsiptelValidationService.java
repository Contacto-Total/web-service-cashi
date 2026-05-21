package com.cashi.osiptel;

import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ContactMethodRepository;
import com.cashi.customermanagement.domain.model.entities.ContactMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio Osiptel (modelo NO-ortogonal V17+).
 *
 * Soporta dos modelos de operacion:
 *   - Pull: el worker hace polling. getPendingQueue() + applyResult().
 *   - Push (legado): validate() — backend llama al worker directamente.
 */
@Service
public class OsiptelValidationService {

    private static final Logger log = LoggerFactory.getLogger(OsiptelValidationService.class);

    private final OsiptelClient client;
    private final ContactMethodRepository contactMethodRepository;
    private final JdbcTemplate jdbcTemplate;

    public OsiptelValidationService(OsiptelClient client,
                                    ContactMethodRepository contactMethodRepository,
                                    JdbcTemplate jdbcTemplate) {
        this.client = client;
        this.contactMethodRepository = contactMethodRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ----- Pull model -----

    public List<OsiptelController.QueueItem> getPendingQueue(int limit) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT mc.id, mc.valor AS phone, c.documento AS dni " +
            "FROM cashi_db.metodos_contacto mc " +
            "JOIN cashi_db.clientes c ON c.id = mc.id_cliente " +
            "WHERE mc.tipo_contacto = 'telefono' AND mc.estado_osiptel = 'SIN_VALIDAR' " +
            "LIMIT ?", limit);

        return rows.stream()
            .map(r -> new OsiptelController.QueueItem(
                ((Number) r.get("id")).longValue(),
                (String) r.get("phone"),
                (String) r.get("dni"),
                "DNI"))
            .collect(Collectors.toList());
    }

    public void applyResult(Long id, String status, String operator) {
        if ("VALIDADO".equals(status) || "NO_VALIDADO".equals(status)) {
            jdbcTemplate.update(
                "UPDATE cashi_db.metodos_contacto SET estado_osiptel = ? WHERE id = ?",
                status, id);
            log.info("applyResult: id={} -> {} operator={}", id, status, operator);
        } else {
            log.warn("applyResult: id={} status={} (sin update, queda SIN_VALIDAR para reintento)", id, status);
        }
    }

    // ----- Push model (legado) -----

    @Transactional
    public OsiptelClient.CheckResult validate(Long idMetodoContacto) {
        ContactMethod mc = contactMethodRepository.findById(idMetodoContacto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "metodos_contacto no encontrado: id=" + idMetodoContacto));

        if (!"telefono".equalsIgnoreCase(mc.getContactType())) {
            throw new IllegalArgumentException(
                    "metodos_contacto id=" + idMetodoContacto + " no es de tipo telefono");
        }

        String phone = mc.getValue();
        String dni = mc.getCustomer() != null ? mc.getCustomer().getDocument() : null;

        if (phone == null || phone.isBlank() || dni == null || dni.isBlank()) {
            throw new IllegalArgumentException(
                    "metodos_contacto id=" + idMetodoContacto + ": falta telefono o documento del cliente");
        }

        OsiptelClient.CheckResult result = client.check(phone, dni);

        if ("VALIDADO".equals(result.status()) || "NO_VALIDADO".equals(result.status())) {
            jdbcTemplate.update(
                    "UPDATE cashi_db.metodos_contacto SET estado_osiptel = ? WHERE id = ?",
                    result.status(), idMetodoContacto);
            log.info("estado_osiptel actualizado: id={} -> {} (operator={}, latencyMs={})",
                    idMetodoContacto, result.status(), result.operator(), result.latencyMs());
        } else {
            log.warn("Osiptel worker devolvio ERROR para id={}: {}",
                    idMetodoContacto, result.errorDetail());
        }

        return result;
    }
}
