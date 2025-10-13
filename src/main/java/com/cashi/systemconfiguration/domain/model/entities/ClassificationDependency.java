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
@Table(name = "dependencias_clasificacion", indexes = {
    @Index(name = "idx_dep_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_dep_cartera", columnList = "id_cartera"),
    @Index(name = "idx_dep_padre", columnList = "id_clasificacion_padre"),
    @Index(name = "idx_dep_hijo", columnList = "id_clasificacion_hijo")
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
    @JoinColumn(name = "id_inquilino", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera")
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clasificacion_padre", nullable = false)
    private ClassificationCatalog parentClassification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clasificacion_hijo", nullable = false)
    private ClassificationCatalog childClassification;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dependencia", nullable = false, length = 50)
    private DependencyType dependencyType;

    @Column(name = "es_obligatorio", nullable = false)
    private Boolean isMandatory = false;

    @Column(name = "expresion_condicion", columnDefinition = "JSON")
    private String conditionExpression;

    @Column(name = "orden_visualizacion")
    private Integer displayOrder;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
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
