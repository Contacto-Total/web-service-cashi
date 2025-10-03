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
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenant_code", columnList = "tenant_code", unique = true),
    @Index(name = "idx_tenant_active", columnList = "is_active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", unique = true, nullable = false, length = 50)
    private String tenantCode;

    @Column(name = "tenant_name", nullable = false, length = 255)
    private String tenantName;

    @Column(name = "business_name", length = 255)
    private String businessName;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_concurrent_sessions")
    private Integer maxConcurrentSessions;

    @Column(name = "subscription_tier", length = 50)
    private String subscriptionTier;

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
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
