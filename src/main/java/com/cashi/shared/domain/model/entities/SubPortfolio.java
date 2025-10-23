package com.cashi.shared.domain.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

/**
 * SubPortfolio Entity - Sub-portfolios that belong to a Portfolio
 * This is a separate entity to prevent cyclical relationships
 */
@Entity
@Table(name = "subcarteras", indexes = {
    @Index(name = "idx_subcartera_cartera", columnList = "id_cartera"),
    @Index(name = "idx_codigo_subcartera", columnList = "id_cartera, codigo_subcartera", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class SubPortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cartera", nullable = false)
    private Portfolio portfolio;

    @Size(max = 3, message = "El código de la subcartera debe tener máximo 3 caracteres")
    @Column(name = "codigo_subcartera", nullable = false, length = 3)
    private String subPortfolioCode;

    @Column(name = "nombre_subcartera", nullable = false, length = 255)
    private String subPortfolioName;

    @Column(name = "descripcion", length = 255)
    private String description;

    @Column(name = "esta_activo", nullable = false)
    private Integer isActive = 1;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDate createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDate updatedAt;

    public SubPortfolio(Portfolio portfolio, String subPortfolioCode, String subPortfolioName) {
        this.portfolio = portfolio;
        this.subPortfolioCode = subPortfolioCode;
        this.subPortfolioName = subPortfolioName;
        this.isActive = 1;
    }

    public SubPortfolio(Portfolio portfolio, String subPortfolioCode, String subPortfolioName, String description) {
        this.portfolio = portfolio;
        this.subPortfolioCode = subPortfolioCode;
        this.subPortfolioName = subPortfolioName;
        this.description = description;
        this.isActive = 1;
    }

    /**
     * Obtiene el tenant a través del portfolio (desnormalizado para conveniencia)
     */
    public Tenant getTenant() {
        return portfolio != null ? portfolio.getTenant() : null;
    }
}
