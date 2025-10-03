package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.Campaign;
import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * CampaignRepository - Spring Data JPA repository for Campaign entity
 */
@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    /**
     * Find campaign by tenant and code
     */
    Optional<Campaign> findByTenantAndCampaignCode(Tenant tenant, String campaignCode);

    /**
     * Find all active campaigns for a tenant
     */
    List<Campaign> findByTenantAndIsActive(Tenant tenant, Boolean isActive);

    /**
     * Find active campaigns for a portfolio
     */
    List<Campaign> findByPortfolioAndIsActive(Portfolio portfolio, Boolean isActive);

    /**
     * Find active campaigns by date range
     */
    @Query("SELECT c FROM Campaign c WHERE c.tenant = :tenant AND c.isActive = true " +
           "AND (c.startDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date))")
    List<Campaign> findActiveCampaignsByDate(@Param("tenant") Tenant tenant, @Param("date") LocalDate date);

    /**
     * Find campaigns by type
     */
    List<Campaign> findByTenantAndCampaignTypeAndIsActive(
        Tenant tenant, Campaign.CampaignType campaignType, Boolean isActive
    );

    /**
     * Check if campaign code exists for tenant
     */
    boolean existsByTenantAndCampaignCode(Tenant tenant, String campaignCode);

    /**
     * Find currently active campaigns (today's date within range)
     */
    @Query("SELECT c FROM Campaign c WHERE c.tenant = :tenant AND c.isActive = true " +
           "AND c.startDate <= CURRENT_DATE AND (c.endDate IS NULL OR c.endDate >= CURRENT_DATE)")
    List<Campaign> findCurrentlyActiveCampaigns(@Param("tenant") Tenant tenant);
}
