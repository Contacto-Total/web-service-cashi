package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.Portfolio;

public interface PortfolioCommandService {
    Portfolio createPortfolio(Long tenantId, String portfolioCode, String portfolioName,
                             String portfolioType, Long parentPortfolioId, String description);
}
