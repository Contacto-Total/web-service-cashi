package com.cashi.wspinbox;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Recibe eventos del Go service (whatsmeow).
 * Cada instancia Go se identifica con el header X-Instancia-Id.
 *
 * Rutas:
 *   POST /api/whatsapp/from-browser  → mensaje entrante o saliente
 *   POST /api/whatsapp/receipt       → estado de entrega
 */
@RestController
@RequestMapping("/api/whatsapp")
public class WspWebhookController {

    public record MediaPayload(
        String id,
        String kind,
        String mime,
        String fileName,
        String caption,
        long   fileLength,
        String url
    ) {}

    public record FromBrowserPayload(
        String       type,           // "INCOMING" | "OUTGOING"
        String       chat,
        String       chatTitle,
        String       text,
        long         ts,
        String       msgId,
        boolean      hasMedia,
        MediaPayload media,
        String       buttonReplyId,
        String       listRowId,
        String       quotedMessageId,
        String       quotedText,
        String       quotedSender,
        Boolean      quotedFromMe
    ) {}

    public record ReceiptPayload(
        String  type,        // "RECEIPT"
        String  chat,
        String  msgId,
        String  status,      // "pending" | "sent" | "delivered" | "read"
        String  participant,
        long    ts
    ) {}

    private final WspInboxService service;

    public WspWebhookController(WspInboxService service) {
        this.service = service;
    }

    @PostMapping("/from-browser")
    public ResponseEntity<Void> fromBrowser(
            @RequestHeader(value = "X-Instancia-Id", defaultValue = "1") String instanciaId,
            @RequestBody FromBrowserPayload payload) {
        service.procesarMensaje(instanciaId, payload);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/receipt")
    public ResponseEntity<Void> receipt(
            @RequestHeader(value = "X-Instancia-Id", defaultValue = "1") String instanciaId,
            @RequestBody ReceiptPayload payload) {
        service.procesarReceipt(payload);
        return ResponseEntity.ok().build();
    }
}
