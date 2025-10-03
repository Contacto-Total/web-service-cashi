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
 * ProcessingStrategy Entity - Strategy pattern implementation for tenant-specific processing
 * Configures which strategy classes to use for different operations per tenant
 */
@Entity
@Table(name = "processing_strategies", indexes = {
    @Index(name = "idx_strategy_tenant", columnList = "tenant_id"),
    @Index(name = "idx_strategy_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_strategy_type", columnList = "strategy_type"),
    @Index(name = "idx_strategy_unique", columnList = "tenant_id, portfolio_id, strategy_type", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ProcessingStrategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false, length = 100)
    private StrategyType strategyType;

    @Column(name = "strategy_implementation", nullable = false, length = 500)
    private String strategyImplementation;

    @Column(name = "strategy_name", length = 255)
    private String strategyName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum StrategyType {
        VALIDATION_STRATEGY,
        FIELD_GENERATION_STRATEGY,
        PAYMENT_PROCESSING_STRATEGY,
        SCHEDULE_CALCULATION_STRATEGY,
        NOTIFICATION_STRATEGY,
        AUTHORIZATION_STRATEGY,
        SCORING_STRATEGY,
        WORKFLOW_STRATEGY
    }

    public ProcessingStrategy(Tenant tenant, StrategyType strategyType, String strategyImplementation) {
        this.tenant = tenant;
        this.strategyType = strategyType;
        this.strategyImplementation = strategyImplementation;
        this.isActive = true;
    }

    public ProcessingStrategy(Tenant tenant, Portfolio portfolio, StrategyType strategyType,
                             String strategyImplementation, String strategyName, String description) {
        this.tenant = tenant;
        this.portfolio = portfolio;
        this.strategyType = strategyType;
        this.strategyImplementation = strategyImplementation;
        this.strategyName = strategyName;
        this.description = description;
        this.isActive = true;
    }
}
