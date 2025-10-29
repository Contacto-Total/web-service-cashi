package com.cashi.customermanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.customermanagement.domain.model.aggregates.Blacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {

    List<Blacklist> findByTenantId(Long tenantId);

    List<Blacklist> findByTenantIdAndPortfolioId(Long tenantId, Long portfolioId);

    List<Blacklist> findByTenantIdAndPortfolioIdAndSubPortfolioId(Long tenantId, Long portfolioId, Long subPortfolioId);
}
