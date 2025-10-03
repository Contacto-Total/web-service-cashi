package com.cashi.shared.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * TenantFieldConfig Entity - Tenant-specific field enablement and configuration
 * Controls which fields are active for each tenant/portfolio
 */
@Entity
@Table(name = "tenant_field_configs", indexes = {
    @Index(name = "idx_tenant_field_tenant", columnList = "tenant_id"),
    @Index(name = "idx_tenant_field_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_tenant_field_def", columnList = "field_definition_id"),
    @Index(name = "idx_tenant_field_unique", columnList = "tenant_id, portfolio_id, field_definition_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class TenantFieldConfig {

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
    @JoinColumn(name = "field_definition_id", nullable = false)
    private FieldDefinition fieldDefinition;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "is_editable", nullable = false)
    private Boolean isEditable = true;

    @Column(name = "display_label", length = 255)
    private String displayLabel;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "default_value_override", length = 500)
    private String defaultValueOverride;

    @Column(name = "validation_rules_override", columnDefinition = "JSON")
    private String validationRulesOverride;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TenantFieldConfig(Tenant tenant, FieldDefinition fieldDefinition, Boolean isEnabled) {
        this.tenant = tenant;
        this.fieldDefinition = fieldDefinition;
        this.isEnabled = isEnabled;
        this.isRequired = false;
        this.isVisible = true;
        this.isEditable = true;
    }

    public TenantFieldConfig(Tenant tenant, Portfolio portfolio, FieldDefinition fieldDefinition,
                            Boolean isEnabled, Boolean isRequired, Boolean isVisible) {
        this.tenant = tenant;
        this.portfolio = portfolio;
        this.fieldDefinition = fieldDefinition;
        this.isEnabled = isEnabled;
        this.isRequired = isRequired;
        this.isVisible = isVisible;
        this.isEditable = true;
    }
}
