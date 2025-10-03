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
 * ClassificationDependency - Define dependencias entre clasificaciones
 * Ejemplo: "Acepta Compromiso Pago" REQUIRES "Fecha de Pago"
 */
@Entity
@Table(name = "classification_dependencies", indexes = {
    @Index(name = "idx_dep_tenant", columnList = "tenant_id"),
    @Index(name = "idx_dep_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_dep_parent", columnList = "parent_classification_id"),
    @Index(name = "idx_dep_child", columnList = "child_classification_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ClassificationDependency {

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
    @JoinColumn(name = "parent_classification_id", nullable = false)
    private ClassificationCatalog parentClassification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_classification_id", nullable = false)
    private ClassificationCatalog childClassification;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 50)
    private DependencyType dependencyType;

    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = false;

    @Column(name = "condition_expression", columnDefinition = "JSON")
    private String conditionExpression;

    @Column(name = "display_order")
    private Integer displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DependencyType {
        REQUIRES,    // Padre requiere hijo (obligatorio)
        SUGGESTS,    // Padre sugiere hijo (opcional, pero recomendado)
        BLOCKS,      // Padre bloquea hijo (no se puede seleccionar)
        ENABLES      // Padre habilita hijo (solo se muestra si padre está seleccionado)
    }

    public ClassificationDependency(Tenant tenant, Portfolio portfolio,
                                   ClassificationCatalog parent, ClassificationCatalog child,
                                   DependencyType dependencyType, Boolean isMandatory) {
        this.tenant = tenant;
        this.portfolio = portfolio;
        this.parentClassification = parent;
        this.childClassification = child;
        this.dependencyType = dependencyType;
        this.isMandatory = isMandatory;
    }
}
