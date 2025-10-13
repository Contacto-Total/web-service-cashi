package com.cashi.systemconfiguration.domain.model.entities;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * TenantClassificationConfig - Configuración de clasificaciones por tenant/portfolio
 * Permite habilitar/deshabilitar y personalizar clasificaciones
 */
@Entity
@Table(name = "configuracion_clasificacion_inquilino", indexes = {
    @Index(name = "idx_config_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_config_cartera", columnList = "id_cartera"),
    @Index(name = "idx_config_clasificacion", columnList = "id_clasificacion"),
    @Index(name = "idx_config_habilitado", columnList = "id_inquilino, id_cartera, esta_habilitado"),
    @Index(name = "idx_config_clasificacion_unico", columnList = "id_inquilino, id_cartera, id_clasificacion", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class TenantClassificationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inquilino", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera")
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clasificacion", nullable = false)
    private ClassificationCatalog classification;

    @Column(name = "esta_habilitado", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "es_requerido", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "nombre_personalizado", length = 255)
    private String customName;

    @Column(name = "orden_personalizado")
    private Integer customOrder;

    @Column(name = "icono_personalizado", length = 100)
    private String customIcon;

    @Column(name = "color_personalizado", length = 7)
    private String customColor;

    @Column(name = "requiere_comentario", nullable = false)
    private Boolean requiresComment = false;

    @Column(name = "longitud_minima_comentario")
    private Integer minCommentLength;

    @Column(name = "requiere_adjunto", nullable = false)
    private Boolean requiresAttachment = false;

    @Column(name = "requiere_fecha_seguimiento", nullable = false)
    private Boolean requiresFollowupDate = false;

    @Column(name = "disparadores_automaticos", columnDefinition = "JSON")
    private String autoTriggers;

    @Column(name = "validation_rules", columnDefinition = "JSON")
    private String validationRules;

    @Column(name = "ui_config", columnDefinition = "JSON")
    private String uiConfig;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TenantClassificationConfig(Tenant tenant, Portfolio portfolio,
                                     ClassificationCatalog classification, Boolean isEnabled) {
        this.tenant = tenant;
        this.portfolio = portfolio;
        this.classification = classification;
        this.isEnabled = isEnabled;
        this.isRequired = false;
        this.requiresComment = false;
        this.requiresAttachment = false;
        this.requiresFollowupDate = false;
    }

    public String getEffectiveName() {
        return customName != null ? customName : classification.getName();
    }

    public Integer getEffectiveOrder() {
        return customOrder != null ? customOrder : classification.getDisplayOrder();
    }

    public String getEffectiveIcon() {
        return customIcon != null ? customIcon : classification.getIconName();
    }

    public String getEffectiveColor() {
        return customColor != null ? customColor : classification.getColorHex();
    }
}
