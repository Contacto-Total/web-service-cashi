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
@Table(name = "campaigns", indexes = {
    @Index(name = "idx_campaign_tenant", columnList = "tenant_id"),
    @Index(name = "idx_campaign_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_campaign_dates", columnList = "start_date, end_date"),
    @Index(name = "idx_campaign_active", columnList = "is_active")
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
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(name = "campaign_code", nullable = false, length = 50)
    private String campaignCode;

    @Column(name = "campaign_name", nullable = false, length = 255)
    private String campaignName;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type", length = 50)
    private CampaignType campaignType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "target_accounts")
    private Integer targetAccounts;

    @Column(name = "target_amount")
    private java.math.BigDecimal targetAmount;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
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
