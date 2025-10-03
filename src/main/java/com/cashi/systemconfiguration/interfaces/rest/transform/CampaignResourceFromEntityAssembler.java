package com.cashi.systemconfiguration.interfaces.rest.transform;

import com.cashi.systemconfiguration.domain.model.aggregates.Campaign;
import com.cashi.systemconfiguration.interfaces.rest.resources.CampaignResource;

public class CampaignResourceFromEntityAssembler {

    public static CampaignResource toResourceFromEntity(Campaign entity) {
        return new CampaignResource(
                entity.getId(),
                entity.getCampaignId(),
                entity.getName(),
                entity.getCampaignType(),
                entity.getIsActive()
        );
    }
}
