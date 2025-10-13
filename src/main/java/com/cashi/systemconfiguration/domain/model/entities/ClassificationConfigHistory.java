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
@Table(name = "historial_configuracion_clasificacion", indexes = {
    @Index(name = "idx_historial_entidad", columnList = "tipo_entidad, id_entidad"),
    @Index(name = "idx_historial_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_historial_fecha", columnList = "fecha_creacion DESC"),
    @Index(name = "idx_historial_usuario", columnList = "cambiado_por")
})
@Getter
@Setter
@NoArgsConstructor
public class ClassificationConfigHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entidad", nullable = false, length = 50)
    private EntityType entityType;

    @Column(name = "id_entidad", nullable = false)
    private Long entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inquilino")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera")
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cambio", nullable = false, length = 50)
    private ChangeType changeType;

    @Column(name = "cambiado_por", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "valor_previo", columnDefinition = "JSON")
    private String previousValue;

    @Column(name = "valor_nuevo", columnDefinition = "JSON")
    private String newValue;

    @Column(name = "razon_cambio", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "direccion_ip", length = 45)
    private String ipAddress;

    @Column(name = "agente_usuario", length = 500)
    private String userAgent;

    @Column(name = "fecha_creacion", nullable = false)
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
