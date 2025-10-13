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
 * TenantBusinessRule Entity - Tenant-specific business rules and validations
 * Implements Strategy pattern configuration for validation rules
 */
@Entity
@Table(name = "reglas_negocio_inquilino", indexes = {
    @Index(name = "idx_regla_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_regla_cartera", columnList = "id_cartera"),
    @Index(name = "idx_regla_codigo", columnList = "codigo_regla"),
    @Index(name = "idx_regla_unico", columnList = "id_inquilino, id_cartera, codigo_regla", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class TenantBusinessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inquilino", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera")
    private Portfolio portfolio;

    @Column(name = "codigo_regla", nullable = false, length = 100)
    private String ruleCode;

    @Column(name = "nombre_regla", nullable = false, length = 255)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_regla", nullable = false, length = 50)
    private RuleType ruleType;

    @Column(name = "categoria_regla", length = 100)
    private String ruleCategory;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "esta_activo", nullable = false)
    private Boolean isActive = true;

    @Column(name = "prioridad")
    private Integer priority;

    @Column(name = "expresion_condicion", columnDefinition = "TEXT")
    private String conditionExpression;

    @Column(name = "expresion_accion", columnDefinition = "TEXT")
    private String actionExpression;

    @Column(name = "json_validacion", columnDefinition = "JSON")
    private String validationJson;

    @Column(name = "mensaje_error", length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    public enum RuleType {
        FIELD_VALIDATION,
        WORKFLOW_VALIDATION,
        AUTHORIZATION_RULE,
        CALCULATION_RULE,
        CONDITIONAL_FIELD,
        AUTO_FILL,
        CROSS_FIELD_VALIDATION,
        BUSINESS_LOGIC
    }

    public TenantBusinessRule(Tenant tenant, String ruleCode, String ruleName, RuleType ruleType) {
        this.tenant = tenant;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.isActive = true;
    }

    public TenantBusinessRule(Tenant tenant, Portfolio portfolio, String ruleCode,
                             String ruleName, RuleType ruleType, String description,
                             String conditionExpression, String actionExpression) {
        this.tenant = tenant;
        this.portfolio = portfolio;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.description = description;
        this.conditionExpression = conditionExpression;
        this.actionExpression = actionExpression;
        this.isActive = true;
    }
}
