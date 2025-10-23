package com.cashi.systemconfiguration.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * TypificationCategory - Catálogo de tipos de clasificación
 * Define los comportamientos y características de cada tipo de clasificación
 * (Pagos, Cronogramas, Reclamos, etc.)
 */
@Entity
@Table(name = "categorias_tipificacion", indexes = {
    @Index(name = "idx_categoria_codigo", columnList = "categoria, codigo", unique = true),
    @Index(name = "idx_categoria", columnList = "categoria")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class TypificationCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "categoria", nullable = false, length = 50)
    private String category;

    @Column(name = "codigo", nullable = false, length = 50)
    private String code;

    @Column(name = "nombre", nullable = false, length = 255)
    private String name;

    @Column(name = "nombre_corto", length = 100)
    private String shortName;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "descripcion_usuario", columnDefinition = "TEXT")
    private String userDescription;

    // Comportamiento para PAGOS
    @Column(name = "sugiere_monto_completo", nullable = false)
    private Boolean suggestsFullAmount = false;

    @Column(name = "permite_seleccionar_cuotas", nullable = false)
    private Boolean allowsInstallmentSelection = false;

    @Column(name = "requiere_monto_manual", nullable = false)
    private Boolean requiresManualAmount = false;

    // Comportamiento general
    @Column(name = "requiere_observaciones", nullable = false)
    private Boolean requiresObservations = false;

    @Column(name = "permite_adjuntar_archivo", nullable = false)
    private Boolean allowsFileAttachment = false;

    @Column(name = "dias_vigencia")
    private Integer validityDays;

    // Auditoría
    @Column(name = "esta_activo", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    public TypificationCategory(String category, String code, String name, String description) {
        this.category = category;
        this.code = code;
        this.name = name;
        this.description = description;
        this.isActive = true;
    }

    public TypificationCategory(String category, String code, String name, String shortName,
                                    String description, String userDescription) {
        this(category, code, name, description);
        this.shortName = shortName;
        this.userDescription = userDescription;
    }
}
