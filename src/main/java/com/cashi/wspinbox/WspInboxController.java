package com.cashi.wspinbox;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API REST para el Angular del inbox compartido.
 *
 *   GET /api/v1/wsp/conversaciones              → lista de hilos ordenados por actividad
 *   GET /api/v1/wsp/conversaciones/{id}/mensajes → mensajes de un hilo
 */
@RestController
@RequestMapping("/api/v1/wsp")
public class WspInboxController {

    public record ConversacionDto(
        Long   id,
        String chatJid,
        String chatTitulo,
        String estado,
        String instanciaId,
        String ultimaActividad
    ) {
        static ConversacionDto from(WspConversacion c) {
            return new ConversacionDto(
                c.getId(),
                c.getChatJid(),
                c.getChatTitulo(),
                c.getEstado().name(),
                c.getInstanciaId(),
                c.getUltimaActividad().toString()
            );
        }
    }

    public record MensajeDto(
        Long    id,
        String  wspMsgId,
        String  direccion,
        String  texto,
        boolean tieneMedia,
        String  mediaTipo,
        String  mediaUrl,
        String  mediaMime,
        String  mediaNombre,
        String  quotedMsgId,
        String  quotedTexto,
        Long    asesoraId,
        String  asesoraNombre,
        String  estadoEntrega,
        long    timestampWsp
    ) {
        static MensajeDto from(WspMensaje m) {
            return new MensajeDto(
                m.getId(),
                m.getWspMsgId(),
                m.getDireccion().name(),
                m.getTexto(),
                m.isTieneMedia(),
                m.getMediaTipo(),
                m.getMediaUrl(),
                m.getMediaMime(),
                m.getMediaNombre(),
                m.getQuotedMsgId(),
                m.getQuotedTexto(),
                m.getAsesora() != null ? m.getAsesora().getId()     : null,
                m.getAsesora() != null ? m.getAsesora().getNombre() : null,
                m.getEstadoEntrega().name(),
                m.getTimestampWsp()
            );
        }
    }

    private final WspInboxService service;

    public WspInboxController(WspInboxService service) {
        this.service = service;
    }

    @GetMapping("/conversaciones")
    public ResponseEntity<List<ConversacionDto>> conversaciones() {
        List<ConversacionDto> result = service.listarConversaciones()
            .stream()
            .map(ConversacionDto::from)
            .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/conversaciones/{id}/mensajes")
    public ResponseEntity<List<MensajeDto>> mensajes(@PathVariable Long id) {
        List<MensajeDto> result = service.listarMensajes(id)
            .stream()
            .map(MensajeDto::from)
            .toList();
        return ResponseEntity.ok(result);
    }

    // ------------------------------------------------------------------ //
    //  Hidratación para el Go service: estado completo de una instancia  //
    //  El Go pierde su estado en RAM al reiniciar; aquí lo reconstruye.  //
    // ------------------------------------------------------------------ //

    public record HydrateConversacionDto(
        String              chatJid,
        String              chatTitulo,
        String              instanciaId,
        long                ultimaActividad,
        List<MensajeDto>    mensajes
    ) {}

    // Hydrate: solo metadatos de conversaciones (sin mensajes).
    // Los mensajes se cargan on-demand desde /messages/{jid}.
    @GetMapping("/hydrate")
    public ResponseEntity<List<HydrateConversacionDto>> hydrate(
            @RequestParam(name = "instanciaId", defaultValue = "1") String instanciaId) {

        List<HydrateConversacionDto> result = service.listarConversacionesPorInstancia(instanciaId)
            .stream()
            .map(c -> new HydrateConversacionDto(
                c.getChatJid(),
                c.getChatTitulo(),
                c.getInstanciaId(),
                c.getUltimaActividad()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli(),
                List.of()))  // mensajes vacíos: se cargan on-demand
            .toList();

        return ResponseEntity.ok(result);
    }

    // Mensajes de un chat por JID con paginación.
    // El Go service llama esto on-demand cuando no tiene mensajes en RAM.
    @GetMapping("/messages/{jid}")
    public ResponseEntity<List<MensajeDto>> messagesByJid(
            @PathVariable String jid,
            @RequestParam(name = "limit",  defaultValue = "60")  int limit,
            @RequestParam(name = "before", defaultValue = "0")   long before) {

        List<MensajeDto> result = service.listarMensajesPorJid(jid, limit, before)
            .stream()
            .map(MensajeDto::from)
            .toList();
        return ResponseEntity.ok(result);
    }
}
