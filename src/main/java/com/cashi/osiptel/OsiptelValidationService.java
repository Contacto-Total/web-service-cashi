package com.cashi.osiptel;

import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ContactMethodRepository;
import com.cashi.customermanagement.domain.model.entities.ContactMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio chico que orquesta el flujo Osiptel del modelo NO-ortogonal V17+:
 *   1) Cargar metodos_contacto por id, obtener telefono (valor) y DNI del cliente.
 *   2) Llamar al worker via OsiptelClient.
 *   3) Si el worker responde VALIDADO/NO_VALIDADO, hacer UPDATE de
 *      metodos_contacto.estado_osiptel. Si responde ERROR, no actualiza.
 *
 * Devuelve el CheckResult para que el controller lo serialice al cliente.
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
                    result.status(), idMetodoContacto
            );
            log.info("estado_osiptel actualizado: id={} -> {} (operator={}, latencyMs={})",
                    idMetodoContacto, result.status(), result.operator(), result.latencyMs());
        } else {
            log.warn("Osiptel worker devolvio ERROR para id={}: {}",
                    idMetodoContacto, result.errorDetail());
        }

        return result;
    }
}
