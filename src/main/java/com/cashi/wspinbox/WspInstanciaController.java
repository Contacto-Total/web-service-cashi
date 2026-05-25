package com.cashi.wspinbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Admin: gestión de instancias Go + proxy de /status y /qr.
 *
 * GET  /api/v1/wsp/instancias              → lista instancias activas
 * POST /api/v1/wsp/instancias              → registrar nueva instancia
 * GET  /api/v1/wsp/instancias/{id}/status  → proxy al Go /status
 * GET  /api/v1/wsp/instancias/{id}/qr      → proxy al Go /qr (PNG base64)
 */
@RestController
@RequestMapping("/api/v1/wsp/instancias")
public class WspInstanciaController {

    private final WspInstanciaRepository repo;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper    = new ObjectMapper();

    public WspInstanciaController(WspInstanciaRepository repo) {
        this.repo = repo;
    }

    public record InstanciaDto(String id, String nombre, String baseUrl, boolean activa) {
        static InstanciaDto from(WspInstancia i) {
            return new InstanciaDto(i.getId(), i.getNombre(), i.getBaseUrl(), i.isActiva());
        }
    }

    public record NuevaInstanciaRequest(String id, String nombre, String baseUrl) {}

    @GetMapping
    public ResponseEntity<List<InstanciaDto>> listar() {
        List<InstanciaDto> result = repo.findByActivaTrue()
            .stream().map(InstanciaDto::from).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<InstanciaDto> crear(@RequestBody NuevaInstanciaRequest req) {
        WspInstancia inst = new WspInstancia();
        inst.setId(req.id());
        inst.setNombre(req.nombre());
        inst.setBaseUrl(req.baseUrl());
        inst = repo.save(inst);
        return ResponseEntity.ok(InstanciaDto.from(inst));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Object> status(@PathVariable String id) {
        return proxy(id, "/status");
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<Object> qr(@PathVariable String id) {
        return proxy(id, "/qr");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable String id) {
        repo.findById(id).ifPresent(i -> { i.setActiva(false); repo.save(i); });
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ //

    private ResponseEntity<Object> proxy(String instanciaId, String path) {
        return repo.findById(instanciaId).map(inst -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(inst.getBaseUrl() + path))
                    .GET().build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                Object body = mapper.readValue(resp.body(), Object.class);
                return ResponseEntity.status(resp.statusCode()).body(body);
            } catch (Exception e) {
                return ResponseEntity.ok()
                    .<Object>body(Map.of("ok", false, "error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
