package com.cashi.wspinbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wsp_usuarios", schema = "cashi_db")
public class WspUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.ASESORA;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public enum Rol { ASESORA, SUPERVISOR }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public Rol getRol() { return rol; }
    public boolean isActiva() { return activa; }
    public LocalDateTime getCreadoEn() { return creadoEn; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setEmail(String email) { this.email = email; }
    public void setRol(Rol rol) { this.rol = rol; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
