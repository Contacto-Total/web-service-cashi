package com.cashi.customermanagement.application.internal.queryservices;

import com.cashi.customermanagement.domain.model.aggregates.Blacklist;
import com.cashi.customermanagement.domain.services.BlacklistQueryService;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlacklistQueryServiceImpl implements BlacklistQueryService {

    private final BlacklistRepository blacklistRepository;

    @Override
    public List<Blacklist> getAllBlacklists() {
        return blacklistRepository.findAll();
    }

    @Override
    public Optional<Blacklist> getBlacklistById(Long id) {
        return blacklistRepository.findById(id);
    }

    @Override
    public List<Blacklist> getBlacklistsByTenant(Long tenantId) {
        return blacklistRepository.findByTenantId(tenantId);
    }

    @Override
    public List<Blacklist> getBlacklistsByTenantAndPortfolio(Long tenantId, Long portfolioId) {
        return blacklistRepository.findByTenantIdAndPortfolioId(tenantId, portfolioId);
    }

    @Override
    public List<Blacklist> getBlacklistsByTenantAndPortfolioAndSubPortfolio(Long tenantId, Long portfolioId, Long subPortfolioId) {
        return blacklistRepository.findByTenantIdAndPortfolioIdAndSubPortfolioId(tenantId, portfolioId, subPortfolioId);
    }
}
