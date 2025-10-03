package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.systemconfiguration.domain.model.aggregates.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @deprecated Use com.cashi.shared.infrastructure.persistence.jpa.repositories.CampaignRepository instead
 */
@Deprecated(forRemoval = true)
@Repository("legacyCampaignRepository")
public interface LegacyCampaignRepository extends JpaRepository<Campaign, Long> {
    Optional<Campaign> findByCampaignId(String campaignId);
    List<Campaign> findByIsActive(Boolean isActive);
    List<Campaign> findByCampaignType(String campaignType);
    boolean existsByCampaignId(String campaignId);
}
