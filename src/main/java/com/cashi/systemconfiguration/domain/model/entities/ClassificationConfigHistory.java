package com.cashi.systemconfiguration.domain.model.entities;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ClassificationConfigHistory - Auditoría completa de cambios en configuración
 * Permite trazabilidad y rollback
 */
@Entity
@Table(name = "classification_config_history", indexes = {
    @Index(name = "idx_history_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_history_tenant", columnList = "tenant_id"),
    @Index(name = "idx_history_date", columnList = "created_at DESC"),
    @Index(name = "idx_history_user", columnList = "changed_by")
})
@Getter
@Setter
@NoArgsConstructor
public class ClassificationConfigHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 50)
    private ChangeType changeType;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "previous_value", columnDefinition = "JSON")
    private String previousValue;

    @Column(name = "new_value", columnDefinition = "JSON")
    private String newValue;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum EntityType {
        CLASSIFICATION,
        CONFIG,
        DEPENDENCY,
        FIELD_MAPPING,
        VERSION
    }

    public enum ChangeType {
        CREATE,
        UPDATE,
        DELETE,
        ENABLE,
        DISABLE,
        RESTORE
    }

    public ClassificationConfigHistory(EntityType entityType, Long entityId,
                                      ChangeType changeType, String changedBy,
                                      String previousValue, String newValue) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.changeType = changeType;
        this.changedBy = changedBy;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
