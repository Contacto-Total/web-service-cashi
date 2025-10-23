package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * TenantRepository - Spring Data JPA repository for Tenant entity
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Integer> {

    /**
     * Find tenant by tenant code
     */
    Optional<Tenant> findByTenantCode(String tenantCode);

    /**
     * Find tenant by tenant code and active status
     */
    Optional<Tenant> findByTenantCodeAndIsActive(String tenantCode, Integer isActive);

    /**
     * Check if tenant code exists
     */
    boolean existsByTenantCode(String tenantCode);

    /**
     * Find all active tenants
     */
    @Query("SELECT t FROM Tenant t WHERE t.isActive = 1 ORDER BY t.tenantName")
    java.util.List<Tenant> findAllActive();
}
