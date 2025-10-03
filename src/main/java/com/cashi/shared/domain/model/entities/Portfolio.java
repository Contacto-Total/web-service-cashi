package com.cashi.shared.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Portfolio Entity - Client portfolios with hierarchical support
 * Supports portfolio types and sub-portfolios
 */
@Entity
@Table(name = "portfolios", indexes = {
    @Index(name = "idx_portfolio_tenant", columnList = "tenant_id"),
    @Index(name = "idx_portfolio_parent", columnList = "parent_portfolio_id"),
    @Index(name = "idx_portfolio_code", columnList = "tenant_id, portfolio_code", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "portfolio_code", nullable = false, length = 50)
    private String portfolioCode;

    @Column(name = "portfolio_name", nullable = false, length = 255)
    private String portfolioName;

    @Enumerated(EnumType.STRING)
    @Column(name = "portfolio_type", length = 50)
    private PortfolioType portfolioType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_portfolio_id")
    private Portfolio parentPortfolio;

    @Column(name = "hierarchy_level")
    private Integer hierarchyLevel;

    @Column(name = "hierarchy_path", length = 500)
    private String hierarchyPath;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PortfolioType {
        CREDIT_CARD,
        PERSONAL_LOAN,
        MORTGAGE,
        AUTO_LOAN,
        COMMERCIAL,
        RETAIL,
        TELECOM,
        UTILITIES,
        EDUCATION,
        OTHER
    }

    public Portfolio(Tenant tenant, String portfolioCode, String portfolioName, PortfolioType portfolioType) {
        this.tenant = tenant;
        this.portfolioCode = portfolioCode;
        this.portfolioName = portfolioName;
        this.portfolioType = portfolioType;
        this.isActive = true;
        this.hierarchyLevel = 1;
    }

    public Portfolio(Tenant tenant, String portfolioCode, String portfolioName,
                    PortfolioType portfolioType, Portfolio parentPortfolio) {
        this.tenant = tenant;
        this.portfolioCode = portfolioCode;
        this.portfolioName = portfolioName;
        this.portfolioType = portfolioType;
        this.parentPortfolio = parentPortfolio;
        this.isActive = true;

        if (parentPortfolio != null) {
            this.hierarchyLevel = parentPortfolio.getHierarchyLevel() + 1;
            this.hierarchyPath = parentPortfolio.getHierarchyPath() + "/" + portfolioCode;
        } else {
            this.hierarchyLevel = 1;
            this.hierarchyPath = "/" + portfolioCode;
        }
    }
}
