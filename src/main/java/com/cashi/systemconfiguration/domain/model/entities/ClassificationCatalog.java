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
 * ClassificationCatalog - Catálogo unificado de TODAS las tipificaciones
 * Soporta jerarquías de N niveles (nivel 1, 2, 3, ... N)
 */
@Entity
@Table(name = "catalogo_clasificaciones", indexes = {
    @Index(name = "idx_codigo_clasificacion", columnList = "codigo", unique = true),
    @Index(name = "idx_tipo_clasificacion", columnList = "tipo_clasificacion"),
    @Index(name = "idx_clasificacion_padre", columnList = "id_clasificacion_padre"),
    @Index(name = "idx_jerarquia_clasificacion", columnList = "nivel_jerarquia, orden_visualizacion")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ClassificationCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", unique = true, nullable = false, length = 20)
    private String code;

    @Column(name = "nombre", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_clasificacion", nullable = false, length = 50)
    private ClassificationType classificationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clasificacion_padre")
    private ClassificationCatalog parentClassification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_clasificacion")
    private ClassificationTypeCatalog classificationTypeCatalog;

    @Column(name = "nivel_jerarquia", nullable = false)
    private Integer hierarchyLevel;

    @Column(name = "ruta_jerarquia", length = 1000)
    private String hierarchyPath;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "orden_visualizacion")
    private Integer displayOrder;

    @Column(name = "nombre_icono", length = 100)
    private String iconName;

    @Column(name = "color_hexadecimal", length = 7)
    private String colorHex;

    @Column(name = "es_sistema", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "esquema_metadatos", columnDefinition = "JSON")
    private String metadataSchema;

    @Column(name = "esta_activo", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime deletedAt;

    public enum ClassificationType {
        CONTACT_RESULT,      // Resultado de contacto (CPC, CTT, etc.)
        MANAGEMENT_TYPE,     // Tipo de gestión (ACP, RCP, etc.)
        PAYMENT_TYPE,        // Tipo de pago
        COMPLAINT_TYPE,      // Tipo de reclamo
        PAYMENT_SCHEDULE,    // Cronograma de pagos
        CUSTOM               // Personalizados por tenant
    }

    public ClassificationCatalog(String code, String name, ClassificationType classificationType) {
        this.code = code;
        this.name = name;
        this.classificationType = classificationType;
        this.hierarchyLevel = 1;
        this.hierarchyPath = "/" + code;
        this.isActive = true;
        this.isSystem = false;
    }

    public ClassificationCatalog(String code, String name, ClassificationType classificationType,
                                ClassificationCatalog parent, Integer displayOrder) {
        this.code = code;
        this.name = name;
        this.classificationType = classificationType;
        this.parentClassification = parent;
        this.displayOrder = displayOrder;
        this.isActive = true;
        this.isSystem = false;

        if (parent != null) {
            this.hierarchyLevel = parent.getHierarchyLevel() + 1;
            this.hierarchyPath = parent.getHierarchyPath() + "/" + code;
        } else {
            this.hierarchyLevel = 1;
            this.hierarchyPath = "/" + code;
        }
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
    }

    public void restore() {
        this.deletedAt = null;
        this.isActive = true;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isRootLevel() {
        return hierarchyLevel == 1;
    }

    public boolean hasParent() {
        return parentClassification != null;
    }
}
