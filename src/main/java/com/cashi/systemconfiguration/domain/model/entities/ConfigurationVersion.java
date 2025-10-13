package com.cashi.systemconfiguration.domain.model.entities;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ConfigurationVersion - Versionado de configuraciones
 * Permite snapshots y rollback completo
 */
@Entity
@Table(name = "versiones_configuracion", indexes = {
    @Index(name = "idx_version_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_version_cartera", columnList = "id_cartera"),
    @Index(name = "idx_version_numero", columnList = "id_inquilino, id_cartera, numero_version", unique = true),
    @Index(name = "idx_version_activo", columnList = "id_inquilino, id_cartera, esta_activo")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ConfigurationVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inquilino", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera")
    private Portfolio portfolio;

    @Column(name = "numero_version", nullable = false)
    private Integer versionNumber;

    @Column(name = "nombre_version", nullable = false, length = 255)
    private String versionName;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "esta_activo", nullable = false)
    private Boolean isActive = false;

    @Column(name = "datos_snapshot", columnDefinition = "LONGTEXT")
    private String snapshotData;

    @Column(name = "creado_por", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "activado_en")
    private LocalDateTime activatedAt;

    @Column(name = "activado_por", length = 100)
    private String activatedBy;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ConfigurationVersion(Tenant tenant, Portfolio portfolio,
                               Integer versionNumber, String versionName,
                               String snapshotData, String createdBy) {
        this.tenant = tenant;
        this.portfolio = portfolio;
        this.versionNumber = versionNumber;
        this.versionName = versionName;
        this.snapshotData = snapshotData;
        this.createdBy = createdBy;
        this.isActive = false;
    }

    public void activate(String activatedBy) {
        this.isActive = true;
        this.activatedAt = LocalDateTime.now();
        this.activatedBy = activatedBy;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
