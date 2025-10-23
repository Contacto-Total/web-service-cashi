package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.systemconfiguration.domain.model.commands.UpdatePortfolioCommand;

public interface PortfolioCommandService {
    Portfolio createPortfolio(Integer tenantId, String portfolioCode, String portfolioName,
                             String description);

    Portfolio updatePortfolio(Integer portfolioId, UpdatePortfolioCommand command);

    void deletePortfolio(Integer portfolioId);
}
