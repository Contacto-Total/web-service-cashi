package com.cashi.collectionmanagement.domain.model.entities;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationCatalog;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ManagementClassification - Vincula gestiones con clasificaciones multinivel
 * Soporta jerarquías de N niveles (nivel 1, 2, 3, ... N)
 */
@Entity(name = "ManagementClassificationEntity")
@Table(name = "clasificaciones_gestion", indexes = {
    @Index(name = "idx_clas_gest_gestion", columnList = "id_gestion"),
    @Index(name = "idx_clas_gest_clasificacion", columnList = "id_clasificacion"),
    @Index(name = "idx_clas_gest_nivel", columnList = "id_gestion, nivel_jerarquia"),
    @Index(name = "idx_clas_gest_unico", columnList = "id_gestion, nivel_jerarquia", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ManagementClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestion", nullable = false)
    private Management management;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clasificacion", nullable = false)
    private ClassificationCatalog classification;

    @Column(name = "nivel_jerarquia", nullable = false)
    private Integer hierarchyLevel;

    @CreatedDate
    @Column(name = "seleccionado_en", nullable = false, updatable = false)
    private LocalDateTime selectedAt;

    public ManagementClassification(Management management, ClassificationCatalog classification,
                                   Integer hierarchyLevel) {
        this.management = management;
        this.classification = classification;
        this.hierarchyLevel = hierarchyLevel;
    }
}
