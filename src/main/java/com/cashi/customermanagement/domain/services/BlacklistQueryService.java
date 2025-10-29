package com.cashi.customermanagement.domain.services;

import com.cashi.customermanagement.domain.model.aggregates.Blacklist;

import java.util.List;
import java.util.Optional;

public interface BlacklistQueryService {
    List<Blacklist> getAllBlacklists();
    Optional<Blacklist> getBlacklistById(Long id);
    List<Blacklist> getBlacklistsByTenant(Long tenantId);
    List<Blacklist> getBlacklistsByTenantAndPortfolio(Long tenantId, Long portfolioId);
    List<Blacklist> getBlacklistsByTenantAndPortfolioAndSubPortfolio(Long tenantId, Long portfolioId, Long subPortfolioId);
}
