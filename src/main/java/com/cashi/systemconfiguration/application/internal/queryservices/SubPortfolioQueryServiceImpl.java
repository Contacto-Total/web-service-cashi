package com.cashi.systemconfiguration.application.internal.queryservices;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.TenantRepository;
import com.cashi.systemconfiguration.domain.services.SubPortfolioQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class SubPortfolioQueryServiceImpl implements SubPortfolioQueryService {

    private final SubPortfolioRepository subPortfolioRepository;
    private final PortfolioRepository portfolioRepository;
    private final TenantRepository tenantRepository;

    public SubPortfolioQueryServiceImpl(SubPortfolioRepository subPortfolioRepository,
                                       PortfolioRepository portfolioRepository,
                                       TenantRepository tenantRepository) {
        this.subPortfolioRepository = subPortfolioRepository;
        this.portfolioRepository = portfolioRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public List<SubPortfolio> getAllSubPortfolios() {
        return subPortfolioRepository.findAllWithPortfolio();
    }

    @Override
    public List<SubPortfolio> getSubPortfoliosByPortfolio(Integer portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio no encontrado con ID: " + portfolioId));

        return subPortfolioRepository.findByPortfolio(portfolio);
    }

    @Override
    public List<SubPortfolio> getActiveSubPortfoliosByPortfolio(Integer portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio no encontrado con ID: " + portfolioId));

        return subPortfolioRepository.findByPortfolio(portfolio);
    }

    @Override
    public List<SubPortfolio> getSubPortfoliosByTenant(Integer tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado con ID: " + tenantId));

        return subPortfolioRepository.findByPortfolio_Tenant(tenant);
    }

    @Override
    public Optional<SubPortfolio> getSubPortfolioById(Integer subPortfolioId) {
        return subPortfolioRepository.findById(subPortfolioId);
    }
}
