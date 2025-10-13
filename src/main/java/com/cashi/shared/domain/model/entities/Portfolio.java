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
@Table(name = "carteras", indexes = {
    @Index(name = "idx_cartera_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_cartera_padre", columnList = "id_cartera_padre"),
    @Index(name = "idx_codigo_cartera", columnList = "id_inquilino, codigo_cartera", unique = true)
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
    @JoinColumn(name = "id_inquilino", nullable = false)
    private Tenant tenant;

    @Column(name = "codigo_cartera", nullable = false, length = 50)
    private String portfolioCode;

    @Column(name = "nombre_cartera", nullable = false, length = 255)
    private String portfolioName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cartera", length = 50)
    private PortfolioType portfolioType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera_padre")
    private Portfolio parentPortfolio;

    @Column(name = "nivel_jerarquia")
    private Integer hierarchyLevel;

    @Column(name = "ruta_jerarquia", length = 500)
    private String hierarchyPath;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "esta_activo", nullable = false)
    private Boolean isActive = true;

    @Column(name = "configuracion_json", columnDefinition = "JSON")
    private String configJson;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
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
