package com.cashi.wspinbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wsp_instancias", schema = "cashi_db")
public class WspInstancia {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "base_url", nullable = false, length = 200)
    private String baseUrl;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    public String getId()      { return id; }
    public String getNombre()  { return nombre; }
    public String getBaseUrl() { return baseUrl; }
    public boolean isActiva()  { return activa; }

    public void setId(String id)           { this.id = id; }
    public void setNombre(String nombre)   { this.nombre = nombre; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setActiva(boolean activa)  { this.activa = activa; }
}
