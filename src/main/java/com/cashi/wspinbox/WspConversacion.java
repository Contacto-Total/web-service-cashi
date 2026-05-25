package com.cashi.wspinbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wsp_conversaciones", schema = "cashi_db")
public class WspConversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_jid", nullable = false, unique = true, length = 100)
    private String chatJid;

    @Column(name = "chat_titulo", length = 200)
    private String chatTitulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Estado estado = Estado.ACTIVA;

    @Column(name = "instancia_id", nullable = false, length = 50)
    private String instanciaId = "1";

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    @Column(name = "ultima_actividad", nullable = false)
    private LocalDateTime ultimaActividad = LocalDateTime.now();

    public enum Estado { ACTIVA, CERRADA }

    public Long getId() { return id; }
    public String getChatJid() { return chatJid; }
    public String getChatTitulo() { return chatTitulo; }
    public Estado getEstado() { return estado; }
    public String getInstanciaId() { return instanciaId; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public LocalDateTime getUltimaActividad() { return ultimaActividad; }

    public void setChatJid(String chatJid) { this.chatJid = chatJid; }
    public void setChatTitulo(String chatTitulo) { this.chatTitulo = chatTitulo; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public void setInstanciaId(String instanciaId) { this.instanciaId = instanciaId; }
    public void setUltimaActividad(LocalDateTime ultimaActividad) { this.ultimaActividad = ultimaActividad; }
}
