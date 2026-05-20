package com.cashi.whatsapp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints WhatsApp (modelo NO-ortogonal V17+).
 *
 * Pull model: el worker hace polling al backend — no al revés.
 *   GET  /api/v1/whatsapp/queue?limit=N  → devuelve metodos_contacto SIN_VALIDAR
 *   POST /api/v1/whatsapp/result         → recibe resultado del worker, actualiza DB
 *
 * Auth: header X-Worker-Token (igual que el worker Osiptel).
 */
@RestController
@RequestMapping("/api/v1/whatsapp")
@EnableConfigurationProperties(WhatsappProperties.class)
public class WhatsappController {

    public record QueueItem(Long id, String phone) {}
    public record ResultBody(Long id, String status, String errorDetail) {}

    private final WhatsappValidationService service;
    private final WhatsappProperties props;

    public WhatsappController(WhatsappValidationService service, WhatsappProperties props) {
        this.service = service;
        this.props = props;
    }

    @GetMapping("/queue")
    public ResponseEntity<List<QueueItem>> queue(
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader(value = "X-Worker-Token", defaultValue = "") String token) {
        if (!props.getWorkerToken().equals(token)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(service.getPendingQueue(limit));
    }

    @PostMapping("/result")
    public ResponseEntity<Void> result(
            @RequestBody ResultBody body,
            @RequestHeader(value = "X-Worker-Token", defaultValue = "") String token) {
        if (!props.getWorkerToken().equals(token)) return ResponseEntity.status(401).build();
        service.applyResult(body.id(), body.status());
        return ResponseEntity.ok().build();
    }
}
