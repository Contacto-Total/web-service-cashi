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
 * Portfolio Entity - Client portfolios with hierarchical support
 * Supports portfolio types and sub-portfolios
 */
@Entity
@Table(name = "carteras", indexes = {
    @Index(name = "idx_cartera_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_codigo_cartera", columnList = "id_inquilino, codigo_cartera", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inquilino", nullable = false)
    private Tenant tenant;

    @Size(max = 3, message = "El código de la cartera debe tener máximo 3 caracteres")
    @Column(name = "codigo_cartera", nullable = false, length = 3)
    private String portfolioCode;

    @Column(name = "nombre_cartera", nullable = false, length = 255)
    private String portfolioName;

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

    public Portfolio(Tenant tenant, String portfolioCode, String portfolioName) {
        this.tenant = tenant;
        this.portfolioCode = portfolioCode;
        this.portfolioName = portfolioName;
        this.isActive = 1;
    }
}
