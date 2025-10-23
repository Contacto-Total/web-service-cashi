package com.cashi.systemconfiguration.interfaces.rest.transform;

import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.systemconfiguration.interfaces.rest.resources.SubPortfolioResource;

public class SubPortfolioResourceFromEntityAssembler {

    public static SubPortfolioResource toResourceFromEntity(SubPortfolio entity) {
        return new SubPortfolioResource(
            entity.getId(),
            entity.getPortfolio().getId(),
            entity.getPortfolio().getPortfolioCode(),
            entity.getPortfolio().getPortfolioName(),
            entity.getSubPortfolioCode(),
            entity.getSubPortfolioName(),
            entity.getDescription(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
