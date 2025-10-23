package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.SubPortfolio;

public interface SubPortfolioCommandService {
    /**
     * Crea una nueva subcartera
     */
    SubPortfolio createSubPortfolio(Integer portfolioId, String subPortfolioCode,
                                   String subPortfolioName, String description);

    /**
     * Actualiza una subcartera existente
     */
    SubPortfolio updateSubPortfolio(Integer subPortfolioId, String subPortfolioName, String description);

    /**
     * Elimina una subcartera
     */
    void deleteSubPortfolio(Integer subPortfolioId);

    /**
     * Activa o desactiva una subcartera
     */
    SubPortfolio toggleSubPortfolioStatus(Integer subPortfolioId, Integer isActive);
}
