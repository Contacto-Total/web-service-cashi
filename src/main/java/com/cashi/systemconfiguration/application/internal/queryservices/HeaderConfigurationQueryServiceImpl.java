package com.cashi.systemconfiguration.application.internal.queryservices;

import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.HeaderConfigurationRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
import com.cashi.systemconfiguration.domain.services.HeaderConfigurationQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HeaderConfigurationQueryServiceImpl implements HeaderConfigurationQueryService {

    private final HeaderConfigurationRepository headerConfigurationRepository;
    private final SubPortfolioRepository subPortfolioRepository;

    public HeaderConfigurationQueryServiceImpl(
            HeaderConfigurationRepository headerConfigurationRepository,
            SubPortfolioRepository subPortfolioRepository) {
        this.headerConfigurationRepository = headerConfigurationRepository;
        this.subPortfolioRepository = subPortfolioRepository;
    }

    @Override
    public List<HeaderConfiguration> getAllBySubPortfolio(Integer subPortfolioId) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        return headerConfigurationRepository.findBySubPortfolio(subPortfolio);
    }

    @Override
    public List<HeaderConfiguration> getAllBySubPortfolioAndLoadType(Integer subPortfolioId, LoadType loadType) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        return headerConfigurationRepository.findBySubPortfolioAndLoadType(subPortfolio, loadType);
    }

    @Override
    public Optional<HeaderConfiguration> getById(Integer id) {
        return headerConfigurationRepository.findById(id);
    }

    @Override
    public long countBySubPortfolio(Integer subPortfolioId) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        return headerConfigurationRepository.countBySubPortfolio(subPortfolio);
    }
}
