package com.cashi.osiptelvalidation.application.internal.commandservices;

import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash determinístico del DNI con sal GLOBAL.
 * El DNI plaintext nunca toca el almacenamiento; solo se usa para:
 *  - calcular el hash (aquí)
 *  - enviar al worker en el request (no se persiste tras la respuesta)
 *
 * La sal es global (no por tenant) porque el candidate selector necesita
 * comparar dni_hash en SQL contra la tabla osiptel_validation_log via SHA2(),
 * cosa que no funciona con sal variable por fila. La sal global mantiene la
 * propiedad de "el hash sin la sal no es útil para fuerza bruta inversa".
 */
@Component
public class DniHashService {

    private final OsiptelProperties properties;

    public DniHashService(OsiptelProperties properties) {
        this.properties = properties;
    }

    /**
     * SHA-256(dni + ':' + globalSalt). El parámetro tenantId se ignora
     * (legado de un diseño previo con sal per-tenant) - se conserva por
     * compatibilidad de la firma con callers existentes.
     */
    public String hash(String dni, Long tenantId) {
        return hash(dni);
    }

    public String hash(String dni) {
        if (dni == null || dni.isBlank()) {
            return null;
        }
        String salt = properties.getDniHashSalt();
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
