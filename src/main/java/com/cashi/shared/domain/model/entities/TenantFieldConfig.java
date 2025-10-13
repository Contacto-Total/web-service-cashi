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
@Table(name = "configuracion_campos_inquilino", indexes = {
    @Index(name = "idx_config_campo_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_config_campo_cartera", columnList = "id_cartera"),
    @Index(name = "idx_config_campo_def", columnList = "id_definicion_campo"),
    @Index(name = "idx_config_campo_unico", columnList = "id_inquilino, id_cartera, id_definicion_campo", unique = true)
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
    @JoinColumn(name = "id_inquilino", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera")
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_definicion_campo", nullable = false)
    private FieldDefinition fieldDefinition;

    @Column(name = "esta_habilitado", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "es_requerido", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "es_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "es_editable", nullable = false)
    private Boolean isEditable = true;

    @Column(name = "etiqueta_visualizacion", length = 255)
    private String displayLabel;

    @Column(name = "orden_visualizacion")
    private Integer displayOrder;

    @Column(name = "valor_predeterminado_sobrescrito", length = 500)
    private String defaultValueOverride;

    @Column(name = "reglas_validacion_sobrescritas", columnDefinition = "JSON")
    private String validationRulesOverride;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
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
