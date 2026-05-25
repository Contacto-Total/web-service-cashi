package com.cashi.wspinbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wsp_mensajes", schema = "cashi_db")
public class WspMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private WspConversacion conversacion;

    @Column(name = "wsp_msg_id", nullable = false, unique = true, length = 100)
    private String wspMsgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direccion direccion;

    @Column(columnDefinition = "TEXT")
    private String texto;

    @Column(name = "tiene_media", nullable = false)
    private boolean tieneMedia = false;

    @Column(name = "media_tipo", length = 20)
    private String mediaTipo;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Column(name = "media_mime", length = 100)
    private String mediaMime;

    @Column(name = "media_nombre", length = 255)
    private String mediaNombre;

    @Column(name = "quoted_msg_id", length = 100)
    private String quotedMsgId;

    @Column(name = "quoted_texto", columnDefinition = "TEXT")
    private String quotedTexto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asesora_id")
    private WspUsuario asesora;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrega", nullable = false, length = 10)
    private EstadoEntrega estadoEntrega = EstadoEntrega.PENDIENTE;

    @Column(name = "timestamp_wsp", nullable = false)
    private long timestampWsp;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public enum Direccion { ENTRANTE, SALIENTE }

    public enum EstadoEntrega {
        PENDIENTE, ENVIADO, ENTREGADO, LEIDO;

        private static final java.util.Map<String, EstadoEntrega> BY_GO_STATUS = java.util.Map.of(
            "pending",   PENDIENTE,
            "sent",      ENVIADO,
            "delivered", ENTREGADO,
            "read",      LEIDO
        );

        public static EstadoEntrega fromGoStatus(String s) {
            return BY_GO_STATUS.getOrDefault(s, PENDIENTE);
        }

        public boolean isHigherThan(EstadoEntrega other) {
            return this.ordinal() > other.ordinal();
        }
    }

    public Long getId() { return id; }
    public WspConversacion getConversacion() { return conversacion; }
    public String getWspMsgId() { return wspMsgId; }
    public Direccion getDireccion() { return direccion; }
    public String getTexto() { return texto; }
    public boolean isTieneMedia() { return tieneMedia; }
    public String getMediaTipo() { return mediaTipo; }
    public String getMediaUrl() { return mediaUrl; }
    public String getMediaMime() { return mediaMime; }
    public String getMediaNombre() { return mediaNombre; }
    public String getQuotedMsgId() { return quotedMsgId; }
    public String getQuotedTexto() { return quotedTexto; }
    public WspUsuario getAsesora() { return asesora; }
    public EstadoEntrega getEstadoEntrega() { return estadoEntrega; }
    public long getTimestampWsp() { return timestampWsp; }
    public LocalDateTime getCreadoEn() { return creadoEn; }

    public void setConversacion(WspConversacion conversacion) { this.conversacion = conversacion; }
    public void setWspMsgId(String wspMsgId) { this.wspMsgId = wspMsgId; }
    public void setDireccion(Direccion direccion) { this.direccion = direccion; }
    public void setTexto(String texto) { this.texto = texto; }
    public void setTieneMedia(boolean tieneMedia) { this.tieneMedia = tieneMedia; }
    public void setMediaTipo(String mediaTipo) { this.mediaTipo = mediaTipo; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public void setMediaMime(String mediaMime) { this.mediaMime = mediaMime; }
    public void setMediaNombre(String mediaNombre) { this.mediaNombre = mediaNombre; }
    public void setQuotedMsgId(String quotedMsgId) { this.quotedMsgId = quotedMsgId; }
    public void setQuotedTexto(String quotedTexto) { this.quotedTexto = quotedTexto; }
    public void setAsesora(WspUsuario asesora) { this.asesora = asesora; }
    public void setEstadoEntrega(EstadoEntrega estadoEntrega) { this.estadoEntrega = estadoEntrega; }
    public void setTimestampWsp(long timestampWsp) { this.timestampWsp = timestampWsp; }
}
