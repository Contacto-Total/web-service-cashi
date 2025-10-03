package com.cashi.systemconfiguration.domain.model.aggregates;

import com.cashi.shared.domain.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Campaign Aggregate Root
 * Representa una campaña de cobranza
 * @deprecated Use com.cashi.shared.domain.model.entities.Campaign instead
 */
@Deprecated(forRemoval = true)
@Entity(name = "LegacyCampaign")
@Table(name = "legacy_campaigns")
@Getter
@NoArgsConstructor
public class Campaign extends AggregateRoot {

    @Column(name = "campaign_id", unique = true, nullable = false, length = 36)
    private String campaignId;

    @Column(nullable = false)
    private String name;

    @Column(name = "campaign_type", nullable = false, length = 50)
    private String campaignType;

    @Column(name = "is_active")
    private Boolean isActive;

    public Campaign(String campaignId, String name, String campaignType) {
        super();
        this.campaignId = campaignId;
        this.name = name;
        this.campaignType = campaignType;
        this.isActive = true;
    }

    public void activate() {
        this.isActive = true;
        updateTimestamp();
    }

    public void deactivate() {
        this.isActive = false;
        updateTimestamp();
    }

    public void updateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign name cannot be empty");
        }
        this.name = name;
        updateTimestamp();
    }
}
