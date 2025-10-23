package com.cashi.systemconfiguration.interfaces.rest.transform;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.systemconfiguration.interfaces.rest.resources.PortfolioResource;

public class PortfolioResourceFromEntityAssembler {

    public static PortfolioResource toResourceFromEntity(Portfolio entity) {
        return toResourceFromEntity(entity, false);
    }

    public static PortfolioResource toResourceFromEntity(Portfolio entity, boolean hasSubPortfolios) {
        return new PortfolioResource(
            entity.getId(),
            entity.getPortfolioCode(),
            entity.getPortfolioName(),
            entity.getDescription(),
            entity.getIsActive(),
            hasSubPortfolios
        );
    }
}
