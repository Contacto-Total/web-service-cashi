package com.cashi.wspinbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WspInboxService {

    private static final Logger log = LoggerFactory.getLogger(WspInboxService.class);

    private final WspConversacionRepository convRepo;
    private final WspMensajeRepository      msgRepo;
    private final WspUsuarioRepository      usuarioRepo;
    private final WspWebSocketHandler       wsHandler;

    public WspInboxService(WspConversacionRepository convRepo,
                           WspMensajeRepository      msgRepo,
                           WspUsuarioRepository      usuarioRepo,
                           WspWebSocketHandler       wsHandler) {
        this.convRepo    = convRepo;
        this.msgRepo     = msgRepo;
        this.usuarioRepo = usuarioRepo;
        this.wsHandler   = wsHandler;
    }

    // ------------------------------------------------------------------ //
    //  Llamado desde WspWebhookController (mensajes que llegan del Go)    //
    // ------------------------------------------------------------------ //

    @Transactional
    public void procesarMensaje(String instanciaId, WspWebhookController.FromBrowserPayload p) {
        if (p.msgId() == null || p.chat() == null) return;

        if (msgRepo.findByWspMsgId(p.msgId()).isPresent()) return;

        WspConversacion conv = convRepo.findByChatJid(p.chat())
            .orElseGet(() -> crearConversacion(p.chat(), p.chatTitle(), instanciaId));

        conv.setUltimaActividad(LocalDateTime.now());
        if (p.chatTitle() != null && !p.chatTitle().isBlank()) {
            conv.setChatTitulo(p.chatTitle());
        }
        convRepo.save(conv);

        WspMensaje msg = new WspMensaje();
        msg.setConversacion(conv);
        msg.setWspMsgId(p.msgId());
        msg.setDireccion("INCOMING".equals(p.type())
            ? WspMensaje.Direccion.ENTRANTE
            : WspMensaje.Direccion.SALIENTE);
        msg.setTexto(p.text());
        msg.setTimestampWsp(p.ts());
        msg.setEstadoEntrega(WspMensaje.EstadoEntrega.ENVIADO);

        if (p.hasMedia() && p.media() != null) {
            msg.setTieneMedia(true);
            msg.setMediaTipo(p.media().kind());
            msg.setMediaUrl(p.media().url());
            msg.setMediaMime(p.media().mime());
            msg.setMediaNombre(p.media().fileName());
        }

        if (p.quotedMessageId() != null) {
            msg.setQuotedMsgId(p.quotedMessageId());
            msg.setQuotedTexto(p.quotedText());
        }

        msgRepo.save(msg);
        log.debug("procesarMensaje: conv={} msgId={} dir={}", conv.getId(), p.msgId(), msg.getDireccion());

        // Broadcast al Angular via WebSocket
        wsHandler.broadcast(buildWsEvent("MENSAJE", instanciaId, conv, msg, p));
    }

    @Transactional
    public void procesarReceipt(WspWebhookController.ReceiptPayload p) {
        if (p.msgId() == null) return;

        msgRepo.findByWspMsgId(p.msgId()).ifPresent(msg -> {
            WspMensaje.EstadoEntrega nuevo = WspMensaje.EstadoEntrega.fromGoStatus(p.status());
            if (nuevo.isHigherThan(msg.getEstadoEntrega())) {
                msg.setEstadoEntrega(nuevo);
                msgRepo.save(msg);
                log.debug("procesarReceipt: msgId={} -> {}", p.msgId(), nuevo);

                wsHandler.broadcast(java.util.Map.of(
                    "type",        "RECEIPT",
                    "msgId",       p.msgId(),
                    "chat",        p.chat(),
                    "status",      nuevo.name().toLowerCase(),
                    "instanciaId", msg.getConversacion().getInstanciaId()
                ));
            }
        });
    }

    // ------------------------------------------------------------------ //
    //  Llamado desde WspInboxController (consultas del Angular)           //
    // ------------------------------------------------------------------ //

    public List<WspConversacion> listarConversaciones() {
        return convRepo.findAllByOrderByUltimaActividadDesc();
    }

    public List<WspMensaje> listarMensajes(Long conversacionId) {
        return msgRepo.findByConversacionIdOrderByTimestampWspAsc(conversacionId);
    }

    @Transactional(readOnly = true)
    public List<WspConversacion> listarConversacionesPorInstancia(String instanciaId) {
        return convRepo.findByInstanciaIdOrderByUltimaActividadDesc(instanciaId);
    }

    @Transactional(readOnly = true)
    public List<WspMensaje> listarMensajesPorInstancia(String instanciaId) {
        return msgRepo.findByInstanciaWithRelations(instanciaId);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private WspConversacion crearConversacion(String jid, String titulo, String instanciaId) {
        // INSERT IGNORE handles concurrent inserts for the same JID (history sync race)
        convRepo.insertIgnore(jid, titulo != null ? titulo : jid, instanciaId);
        return convRepo.findByChatJid(jid).orElseThrow();
    }

    private java.util.Map<String, Object> buildWsEvent(
            String type, String instanciaId,
            WspConversacion conv, WspMensaje msg,
            WspWebhookController.FromBrowserPayload p) {

        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("type",         type);
        map.put("instanciaId",  instanciaId);
        map.put("convId",       conv.getId());
        map.put("chat",         p.chat());
        map.put("chatTitle",    p.chatTitle());
        map.put("msgId",        p.msgId());
        map.put("direccion",    "INCOMING".equals(p.type()) ? "ENTRANTE" : "SALIENTE");
        map.put("texto",        p.text());
        map.put("ts",           p.ts());
        map.put("hasMedia",     p.hasMedia());
        map.put("status",       msg.getEstadoEntrega().name().toLowerCase());
        return map;
    }
}
