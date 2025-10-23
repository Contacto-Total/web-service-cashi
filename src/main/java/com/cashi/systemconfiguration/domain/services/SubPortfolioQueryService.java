package com.cashi.systemconfiguration.domain.services;

import com.cashi.shared.domain.model.entities.SubPortfolio;

import java.util.List;
import java.util.Optional;

public interface SubPortfolioQueryService {
    /**
     * Obtiene todas las subcarteras
     */
    List<SubPortfolio> getAllSubPortfolios();

    /**
     * Obtiene todas las subcarteras de un portfolio
     */
    List<SubPortfolio> getSubPortfoliosByPortfolio(Integer portfolioId);

    /**
     * Obtiene todas las subcarteras activas de un portfolio
     */
    List<SubPortfolio> getActiveSubPortfoliosByPortfolio(Integer portfolioId);

    /**
     * Obtiene todas las subcarteras de un tenant
     */
    List<SubPortfolio> getSubPortfoliosByTenant(Integer tenantId);

    /**
     * Obtiene una subcartera por ID
     */
    Optional<SubPortfolio> getSubPortfolioById(Integer subPortfolioId);
}
