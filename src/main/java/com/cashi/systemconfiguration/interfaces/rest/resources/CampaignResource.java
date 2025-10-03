package com.cashi.systemconfiguration.interfaces.rest.resources;

public record CampaignResource(
        Long id,
        String campaignId,
        String name,
        String campaignType,
        Boolean isActive
) {
}
