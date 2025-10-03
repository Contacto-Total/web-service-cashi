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
@Table(name = "classification_catalog", indexes = {
    @Index(name = "idx_classification_code", columnList = "code", unique = true),
    @Index(name = "idx_classification_type", columnList = "classification_type"),
    @Index(name = "idx_classification_parent", columnList = "parent_classification_id"),
    @Index(name = "idx_classification_hierarchy", columnList = "hierarchy_level, display_order")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ClassificationCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_type", nullable = false, length = 50)
    private ClassificationType classificationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_classification_id")
    private ClassificationCatalog parentClassification;

    @Column(name = "hierarchy_level", nullable = false)
    private Integer hierarchyLevel;

    @Column(name = "hierarchy_path", length = 1000)
    private String hierarchyPath;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "icon_name", length = 100)
    private String iconName;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "metadata_schema", columnDefinition = "JSON")
    private String metadataSchema;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum ClassificationType {
        CONTACT_RESULT,      // Resultado de contacto (CPC, CTT, etc.)
        MANAGEMENT_TYPE,     // Tipo de gestión (ACP, RCP, etc.)
        PAYMENT_TYPE,        // Tipo de pago
        COMPLAINT_TYPE,      // Tipo de reclamo
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
