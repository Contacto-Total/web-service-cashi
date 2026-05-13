package com.cashi.osiptelvalidation.application.internal.commandservices;

import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash determinístico del DNI con sal por tenant.
 * El DNI plaintext nunca toca el almacenamiento; solo se usa para:
 *  - calcular el hash (aquí)
 *  - enviar al worker en el request (no se persiste tras la respuesta)
 *
 * Si el tenantSalt llegara a fugarse, un atacante con la BD podría
 * fuerza-brutar (~10⁸ DNIs peruanos posibles). Por eso la sal debe ser por tenant
 * y rotarse periódicamente.
 */
@Component
public class DniHashService {

    private final OsiptelProperties properties;

    public DniHashService(OsiptelProperties properties) {
        this.properties = properties;
    }

    public String hash(String dni, Long tenantId) {
        if (dni == null || dni.isBlank()) {
            return null;
        }
        // TODO: cuando exista TenantSaltRepository, derivar sal por tenant; por ahora global.
        String salt = properties.getDniHashSalt() + ":" + (tenantId == null ? "0" : tenantId);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((dni + ":" + salt).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
