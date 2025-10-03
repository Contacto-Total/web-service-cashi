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
@Table(name = "tenant_business_rules", indexes = {
    @Index(name = "idx_rule_tenant", columnList = "tenant_id"),
    @Index(name = "idx_rule_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_rule_code", columnList = "rule_code"),
    @Index(name = "idx_rule_unique", columnList = "tenant_id, portfolio_id, rule_code", unique = true)
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
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType;

    @Column(name = "rule_category", length = 100)
    private String ruleCategory;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "condition_expression", columnDefinition = "TEXT")
    private String conditionExpression;

    @Column(name = "action_expression", columnDefinition = "TEXT")
    private String actionExpression;

    @Column(name = "validation_json", columnDefinition = "JSON")
    private String validationJson;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
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
