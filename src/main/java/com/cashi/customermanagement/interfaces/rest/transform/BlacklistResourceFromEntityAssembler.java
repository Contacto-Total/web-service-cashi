package com.cashi.customermanagement.interfaces.rest.transform;

import com.cashi.customermanagement.domain.model.aggregates.Blacklist;
import com.cashi.customermanagement.interfaces.rest.resources.BlacklistResource;
import org.springframework.stereotype.Component;

@Component
public class BlacklistResourceFromEntityAssembler {

    public BlacklistResource toResourceFromEntity(Blacklist entity) {
        return new BlacklistResource(
                entity.getId(),
                entity.getCustomerId(),
                entity.getTenantId(),
                entity.getTenantName(),
                entity.getPortfolioId(),
                entity.getPortfolioName(),
                entity.getSubPortfolioId(),
                entity.getSubPortfolioName(),
                entity.getDocument(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getStartDate(),
                entity.getEndDate()
        );
    }
}
