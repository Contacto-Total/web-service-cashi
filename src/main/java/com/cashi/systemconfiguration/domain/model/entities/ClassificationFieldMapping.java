package com.cashi.systemconfiguration.domain.model.entities;

import com.cashi.shared.domain.model.entities.FieldDefinition;
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
 * ClassificationFieldMapping - Mapea campos dinámicos a clasificaciones
 * Define qué campos se requieren/muestran para cada tipificación
 */
@Entity
@Table(name = "classification_field_mappings", indexes = {
    @Index(name = "idx_cfm_tenant", columnList = "tenant_id"),
    @Index(name = "idx_cfm_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_cfm_classification", columnList = "classification_id"),
    @Index(name = "idx_cfm_field", columnList = "field_definition_id"),
    @Index(name = "idx_cfm_unique", columnList = "tenant_id, portfolio_id, classification_id, field_definition_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ClassificationFieldMapping {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_definition_id", nullable = false)
    private FieldDefinition fieldDefinition;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "conditional_logic", columnDefinition = "JSON")
    private String conditionalLogic;

    @Column(name = "display_order")
    private Integer displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ClassificationFieldMapping(Tenant tenant, Portfolio portfolio,
                                     ClassificationCatalog classification,
                                     FieldDefinition fieldDefinition,
                                     Boolean isRequired, Boolean isVisible) {
        this.tenant = tenant;
        this.portfolio = portfolio;
        this.classification = classification;
        this.fieldDefinition = fieldDefinition;
        this.isRequired = isRequired;
        this.isVisible = isVisible;
    }
}
