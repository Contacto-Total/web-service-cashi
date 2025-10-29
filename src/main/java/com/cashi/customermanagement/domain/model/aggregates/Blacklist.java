package com.cashi.customermanagement.domain.model.aggregates;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Blacklist Entity
 */
@Entity
@Table(name = "blacklist")
@Getter
@NoArgsConstructor
public class Blacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_cliente")
    private Long customerId;

    @Column(name = "id_inquilino", nullable = false)
    private Long tenantId;

    @Column(name = "nombre_inquilino", length = 100)
    private String tenantName;

    @Column(name = "id_cartera")
    private Long portfolioId;

    @Column(name = "nombre_cartera", length = 100)
    private String portfolioName;

    @Column(name = "id_subcartera")
    private Long subPortfolioId;

    @Column(name = "nombre_subcartera", length = 100)
    private String subPortfolioName;

    @Column(name = "documento", length = 20)
    private String document;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "telefono", length = 20)
    private String phone;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate startDate;

    @Future(message = "La fecha de fin debe ser mayor al día actual")
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate endDate;

    public Blacklist(Long customerId, Long tenantId, String tenantName, Long portfolioId, String portfolioName,
                     Long subPortfolioId, String subPortfolioName, String document,
                     String email, String phone, LocalDate startDate, LocalDate endDate) {
        this.customerId = customerId;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.portfolioId = portfolioId;
        this.portfolioName = portfolioName;
        this.subPortfolioId = subPortfolioId;
        this.subPortfolioName = subPortfolioName;
        this.document = document;
        this.email = email;
        this.phone = phone;
        this.startDate = startDate != null ? startDate : LocalDate.now();
        this.endDate = endDate;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }

    public void setSubPortfolioId(Long subPortfolioId) {
        this.subPortfolioId = subPortfolioId;
    }

    public void setSubPortfolioName(String subPortfolioName) {
        this.subPortfolioName = subPortfolioName;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
