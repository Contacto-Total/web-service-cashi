package com.cashi.systemconfiguration.interfaces.rest.transform;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.systemconfiguration.interfaces.rest.resources.PortfolioResource;

public class PortfolioResourceFromEntityAssembler {

    public static PortfolioResource toResourceFromEntity(Portfolio entity) {
        return new PortfolioResource(
            entity.getId(),
            entity.getPortfolioCode(),
            entity.getPortfolioName(),
            entity.getPortfolioType() != null ? entity.getPortfolioType().name() : null,
            entity.getParentPortfolio() != null ? entity.getParentPortfolio().getId() : null,
            entity.getHierarchyLevel(),
            entity.getDescription(),
            entity.getIsActive()
        );
    }
}
