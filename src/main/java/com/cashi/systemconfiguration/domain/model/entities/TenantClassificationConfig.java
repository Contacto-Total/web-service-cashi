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
@Table(name = "tenant_classification_config", indexes = {
    @Index(name = "idx_tcc_tenant", columnList = "tenant_id"),
    @Index(name = "idx_tcc_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_tcc_classification", columnList = "classification_id"),
    @Index(name = "idx_tcc_enabled", columnList = "tenant_id, portfolio_id, is_enabled"),
    @Index(name = "idx_tcc_unique", columnList = "tenant_id, portfolio_id, classification_id", unique = true)
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
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classification_id", nullable = false)
    private ClassificationCatalog classification;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "custom_name", length = 255)
    private String customName;

    @Column(name = "custom_order")
    private Integer customOrder;

    @Column(name = "custom_icon", length = 100)
    private String customIcon;

    @Column(name = "custom_color", length = 7)
    private String customColor;

    @Column(name = "requires_comment", nullable = false)
    private Boolean requiresComment = false;

    @Column(name = "min_comment_length")
    private Integer minCommentLength;

    @Column(name = "requires_attachment", nullable = false)
    private Boolean requiresAttachment = false;

    @Column(name = "requires_followup_date", nullable = false)
    private Boolean requiresFollowupDate = false;

    @Column(name = "auto_triggers", columnDefinition = "JSON")
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
