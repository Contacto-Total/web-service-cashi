package com.cashi.collectionmanagement.domain.model.entities;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.systemconfiguration.domain.model.entities.TypificationCatalog;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ManagementTypification - Vincula gestiones con clasificaciones multinivel
 * Soporta jerarquías de N niveles (nivel 1, 2, 3, ... N)
 */
@Entity(name = "ManagementTypificationEntity")
@Table(name = "tipificaciones_gestion", indexes = {
    @Index(name = "idx_clas_gest_gestion", columnList = "id_gestion"),
    @Index(name = "idx_clas_gest_tipificacion", columnList = "id_tipificacion"),
    @Index(name = "idx_clas_gest_nivel", columnList = "id_gestion, nivel_jerarquia"),
    @Index(name = "idx_clas_gest_unico", columnList = "id_gestion, nivel_jerarquia", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ManagementTypification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestion", nullable = false)
    private Management management;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipificacion", nullable = false)
    private TypificationCatalog typification;

    @Column(name = "nivel_jerarquia", nullable = false)
    private Integer hierarchyLevel;

    @CreatedDate
    @Column(name = "seleccionado_en", nullable = false, updatable = false)
    private LocalDateTime selectedAt;

    public ManagementTypification(Management management, TypificationCatalog typification,
                                   Integer hierarchyLevel) {
        this.management = management;
        this.typification = typification;
        this.hierarchyLevel = hierarchyLevel;
    }
}
