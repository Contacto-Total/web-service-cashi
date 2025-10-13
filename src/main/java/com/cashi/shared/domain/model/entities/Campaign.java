package com.cashi.shared.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Campaign Entity - Campaign management per tenant/portfolio
 */
@Entity
@Table(name = "campañas", indexes = {
    @Index(name = "idx_campana_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_campana_cartera", columnList = "id_cartera"),
    @Index(name = "idx_campana_fechas", columnList = "fecha_inicio, fecha_fin"),
    @Index(name = "idx_campana_activo", columnList = "esta_activo")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inquilino", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera")
    private Portfolio portfolio;

    @Column(name = "codigo_campana", nullable = false, length = 50)
    private String campaignCode;

    @Column(name = "nombre_campana", nullable = false, length = 255)
    private String campaignName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_campana", length = 50)
    private CampaignType campaignType;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "fecha_inicio")
    private LocalDate startDate;

    @Column(name = "fecha_fin")
    private LocalDate endDate;

    @Column(name = "esta_activo", nullable = false)
    private Boolean isActive = true;

    @Column(name = "cuentas_objetivo")
    private Integer targetAccounts;

    @Column(name = "monto_objetivo")
    private java.math.BigDecimal targetAmount;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    public enum CampaignType {
        EARLY_COLLECTION,
        LATE_COLLECTION,
        RECOVERY,
        RETENTION,
        CUSTOMER_SERVICE,
        SALES,
        SURVEY,
        OTHER
    }

    public Campaign(Tenant tenant, String campaignCode, String campaignName, CampaignType campaignType) {
        this.tenant = tenant;
        this.campaignCode = campaignCode;
        this.campaignName = campaignName;
        this.campaignType = campaignType;
        this.isActive = true;
    }

    public Campaign(Tenant tenant, Portfolio portfolio, String campaignCode,
                   String campaignName, CampaignType campaignType,
                   LocalDate startDate, LocalDate endDate) {
        this.tenant = tenant;
        this.portfolio = portfolio;
        this.campaignCode = campaignCode;
        this.campaignName = campaignName;
        this.campaignType = campaignType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
    }
}
