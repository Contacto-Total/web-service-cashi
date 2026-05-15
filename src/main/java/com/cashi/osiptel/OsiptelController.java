package com.cashi.osiptel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint chico para disparar validacion Osiptel de un metodo_contacto puntual
 * (modelo NO-ortogonal V17+).
 *
 * POST /api/v1/osiptel/validate/{idMetodoContacto}
 *
 * Bloqueante: espera respuesta del worker (hasta worker-timeout-ms, default 100s).
 */
@RestController
@RequestMapping("/api/v1/osiptel")
public class OsiptelController {

    private final OsiptelValidationService service;

    public OsiptelController(OsiptelValidationService service) {
        this.service = service;
    }

    @PostMapping("/validate/{idMetodoContacto}")
    public ResponseEntity<OsiptelClient.CheckResult> validate(
            @PathVariable("idMetodoContacto") Long idMetodoContacto) {

        try {
            return ResponseEntity.ok(service.validate(idMetodoContacto));
        } catch (IllegalArgumentException e) {
            // 400 con el mensaje
            return ResponseEntity.badRequest().body(
                    new OsiptelClient.CheckResult("ERROR", null, e.getMessage(), 0)
            );
        }
    }
}
