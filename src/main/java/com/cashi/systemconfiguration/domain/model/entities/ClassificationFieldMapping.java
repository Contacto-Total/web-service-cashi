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
@Table(name = "mapeos_campo_clasificacion", indexes = {
    @Index(name = "idx_mcf_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_mcf_cartera", columnList = "id_cartera"),
    @Index(name = "idx_mcf_clasificacion", columnList = "id_clasificacion"),
    @Index(name = "idx_mcf_campo", columnList = "id_definicion_campo"),
    @Index(name = "idx_mcf_unico", columnList = "id_inquilino, id_cartera, id_clasificacion, id_definicion_campo", unique = true)
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
    @JoinColumn(name = "id_inquilino", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera")
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clasificacion", nullable = false)
    private ClassificationCatalog classification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_definicion_campo", nullable = false)
    private FieldDefinition fieldDefinition;

    @Column(name = "es_requerido", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "es_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "logica_condicional", columnDefinition = "JSON")
    private String conditionalLogic;

    @Column(name = "orden_visualizacion")
    private Integer displayOrder;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
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
