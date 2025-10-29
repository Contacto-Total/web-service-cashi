package com.cashi.customermanagement.application.internal.commandservices;

import com.cashi.customermanagement.domain.model.aggregates.Blacklist;
import com.cashi.customermanagement.domain.services.BlacklistCommandService;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlacklistCommandServiceImpl implements BlacklistCommandService {

    private final BlacklistRepository blacklistRepository;

    @Override
    @Transactional
    public Blacklist createBlacklist(Blacklist blacklist) {
        return blacklistRepository.save(blacklist);
    }

    @Override
    @Transactional
    public Blacklist updateBlacklist(Long id, Blacklist blacklist) {
        return blacklistRepository.findById(id)
                .map(existing -> {
                    existing.setTenantId(blacklist.getTenantId());
                    existing.setTenantName(blacklist.getTenantName());
                    existing.setPortfolioId(blacklist.getPortfolioId());
                    existing.setPortfolioName(blacklist.getPortfolioName());
                    existing.setSubPortfolioId(blacklist.getSubPortfolioId());
                    existing.setSubPortfolioName(blacklist.getSubPortfolioName());
                    existing.setDocument(blacklist.getDocument());
                    existing.setEmail(blacklist.getEmail());
                    existing.setPhone(blacklist.getPhone());
                    existing.setStartDate(blacklist.getStartDate());
                    existing.setEndDate(blacklist.getEndDate());
                    return blacklistRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Blacklist not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteBlacklist(Long id) {
        blacklistRepository.deleteById(id);
    }
}
