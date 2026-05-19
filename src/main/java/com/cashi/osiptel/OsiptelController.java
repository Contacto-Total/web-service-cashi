package com.cashi.osiptel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints Osiptel (modelo NO-ortogonal V17+).
 *
 * Pull model: el worker hace polling al backend — no al revés.
 *   GET  /api/v1/osiptel/queue?limit=N  → devuelve metodos_contacto SIN_VALIDAR
 *   POST /api/v1/osiptel/result         → recibe resultado del worker, actualiza DB
 *
 * Legado (opcional, para pruebas manuales):
 *   POST /api/v1/osiptel/validate/{id}  → síncrono, llama al worker directamente
 */
@RestController
@RequestMapping("/api/v1/osiptel")
public class OsiptelController {

    public record QueueItem(Long id, String phone, String dni, String dniType) {}
    public record ResultBody(Long id, String status, String operator) {}

    private final OsiptelValidationService service;
    private final OsiptelProperties props;

    public OsiptelController(OsiptelValidationService service, OsiptelProperties props) {
        this.service = service;
        this.props = props;
    }

    // ----- Pull model -----

    @GetMapping("/queue")
    public ResponseEntity<List<QueueItem>> queue(
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader(value = "X-Worker-Token", defaultValue = "") String token) {
        if (!props.getWorkerToken().equals(token)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(service.getPendingQueue(limit));
    }

    @PostMapping("/result")
    public ResponseEntity<Void> result(
            @RequestBody ResultBody body,
            @RequestHeader(value = "X-Worker-Token", defaultValue = "") String token) {
        if (!props.getWorkerToken().equals(token)) return ResponseEntity.status(401).build();
        service.applyResult(body.id(), body.status(), body.operator());
        return ResponseEntity.ok().build();
    }

    // ----- Legado (push model, pruebas manuales) -----

    @PostMapping("/validate/{idMetodoContacto}")
    public ResponseEntity<OsiptelClient.CheckResult> validate(
            @PathVariable("idMetodoContacto") Long idMetodoContacto) {
        try {
            return ResponseEntity.ok(service.validate(idMetodoContacto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new OsiptelClient.CheckResult("ERROR", null, e.getMessage(), 0)
            );
        }
    }
}
