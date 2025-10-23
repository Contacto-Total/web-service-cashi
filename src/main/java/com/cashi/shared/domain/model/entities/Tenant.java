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
 * Tenant Entity - Represents client companies using the system
 * Multi-tenant isolation at the root level
 */
@Entity
@Table(name = "inquilinos", indexes = {
    @Index(name = "idx_codigo_inquilino", columnList = "codigo_inquilino", unique = true),
    @Index(name = "idx_inquilino_activo", columnList = "esta_activo")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(max = 3, message = "El código del inquilino debe tener máximo 3 caracteres")
    @Column(name = "codigo_inquilino", unique = true, nullable = false, length = 3)
    private String tenantCode;

    @Column(name = "nombre_inquilino", nullable = false, length = 255)
    private String tenantName;

    @Column(name = "razon_social", length = 255)
    private String businessName;

    @Column(name = "esta_activo", nullable = false)
    private Integer isActive = 1;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDate createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDate updatedAt;

    public Tenant(String tenantCode, String tenantName) {
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
        this.isActive = 1;
    }

    public Tenant(String tenantCode, String tenantName, String businessName) {
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
        this.businessName = businessName;
        this.isActive = 1;
    }
}
