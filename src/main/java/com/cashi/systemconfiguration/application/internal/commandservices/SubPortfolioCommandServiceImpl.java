package com.cashi.systemconfiguration.application.internal.commandservices;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
import com.cashi.systemconfiguration.domain.services.SubPortfolioCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubPortfolioCommandServiceImpl implements SubPortfolioCommandService {

    private final SubPortfolioRepository subPortfolioRepository;
    private final PortfolioRepository portfolioRepository;

    public SubPortfolioCommandServiceImpl(SubPortfolioRepository subPortfolioRepository,
                                         PortfolioRepository portfolioRepository) {
        this.subPortfolioRepository = subPortfolioRepository;
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    @Transactional
    public SubPortfolio createSubPortfolio(Integer portfolioId, String subPortfolioCode,
                                          String subPortfolioName, String description) {
        // Validar que el portfolio existe
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio no encontrado con ID: " + portfolioId));

        // Validar que el código de subcartera no exista para este portfolio
        if (subPortfolioRepository.existsByPortfolioAndSubPortfolioCode(portfolio, subPortfolioCode)) {
            throw new IllegalArgumentException(
                "Ya existe una subcartera con el código: " + subPortfolioCode + " en este portfolio"
            );
        }

        // Crear la subcartera
        SubPortfolio subPortfolio = new SubPortfolio(portfolio, subPortfolioCode, subPortfolioName, description);

        // Guardar y retornar
        return subPortfolioRepository.save(subPortfolio);
    }

    @Override
    @Transactional
    public SubPortfolio updateSubPortfolio(Integer subPortfolioId, String subPortfolioName, String description) {
        // Buscar la subcartera
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        // Actualizar campos
        if (subPortfolioName != null && !subPortfolioName.isBlank()) {
            subPortfolio.setSubPortfolioName(subPortfolioName);
        }

        if (description != null) {
            subPortfolio.setDescription(description);
        }

        // Guardar y retornar
        return subPortfolioRepository.save(subPortfolio);
    }

    @Override
    @Transactional
    public void deleteSubPortfolio(Integer subPortfolioId) {
        // Buscar la subcartera
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        // Delete subportfolio
        subPortfolioRepository.delete(subPortfolio);
    }

    @Override
    @Transactional
    public SubPortfolio toggleSubPortfolioStatus(Integer subPortfolioId, Integer isActive) {
        // Buscar la subcartera con su portfolio cargado (JOIN FETCH) para evitar LazyInitializationException
        SubPortfolio subPortfolio = subPortfolioRepository.findByIdWithPortfolio(subPortfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada con ID: " + subPortfolioId));

        // Actualizar el estado
        subPortfolio.setIsActive(isActive);

        // Guardar y retornar
        return subPortfolioRepository.save(subPortfolio);
    }
}
