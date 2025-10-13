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
@Table(name = "estrategias_procesamiento", indexes = {
    @Index(name = "idx_estrategia_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_estrategia_cartera", columnList = "id_cartera"),
    @Index(name = "idx_estrategia_tipo", columnList = "tipo_estrategia"),
    @Index(name = "idx_estrategia_unico", columnList = "id_inquilino, id_cartera, tipo_estrategia", unique = true)
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
    @JoinColumn(name = "id_inquilino", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera")
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_estrategia", nullable = false, length = 100)
    private StrategyType strategyType;

    @Column(name = "implementacion_estrategia", nullable = false, length = 500)
    private String strategyImplementation;

    @Column(name = "nombre_estrategia", length = 255)
    private String strategyName;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "esta_activo", nullable = false)
    private Boolean isActive = true;

    @Column(name = "prioridad")
    private Integer priority;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
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
