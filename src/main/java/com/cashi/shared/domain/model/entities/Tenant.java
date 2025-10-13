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
    private Long id;

    @Column(name = "codigo_inquilino", unique = true, nullable = false, length = 50)
    private String tenantCode;

    @Column(name = "nombre_inquilino", nullable = false, length = 255)
    private String tenantName;

    @Column(name = "razon_social", length = 255)
    private String businessName;

    @Column(name = "numero_fiscal", length = 50)
    private String taxId;

    @Column(name = "codigo_pais", length = 3)
    private String countryCode;

    @Column(name = "zona_horaria", length = 50)
    private String timezone;

    @Column(name = "esta_activo", nullable = false)
    private Boolean isActive = true;

    @Column(name = "maximo_usuarios")
    private Integer maxUsers;

    @Column(name = "maximo_sesiones_concurrentes")
    private Integer maxConcurrentSessions;

    @Column(name = "nivel_suscripcion", length = 50)
    private String subscriptionTier;

    @Column(name = "fecha_expiracion_suscripcion")
    private LocalDateTime subscriptionExpiresAt;

    @Column(name = "configuracion_json", columnDefinition = "JSON")
    private String configJson;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    public Tenant(String tenantCode, String tenantName) {
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
        this.isActive = true;
    }

    public Tenant(String tenantCode, String tenantName, String businessName, String taxId,
                  String countryCode, String timezone) {
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
        this.businessName = businessName;
        this.taxId = taxId;
        this.countryCode = countryCode;
        this.timezone = timezone;
        this.isActive = true;
    }
}
