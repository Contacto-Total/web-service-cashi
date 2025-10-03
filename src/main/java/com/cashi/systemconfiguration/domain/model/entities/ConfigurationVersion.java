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
@Table(name = "configuration_versions", indexes = {
    @Index(name = "idx_version_tenant", columnList = "tenant_id"),
    @Index(name = "idx_version_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_version_number", columnList = "tenant_id, portfolio_id, version_number", unique = true),
    @Index(name = "idx_version_active", columnList = "tenant_id, portfolio_id, is_active")
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
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "version_name", nullable = false, length = 255)
    private String versionName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "snapshot_data", columnDefinition = "LONGTEXT")
    private String snapshotData;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "activated_by", length = 100)
    private String activatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
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
