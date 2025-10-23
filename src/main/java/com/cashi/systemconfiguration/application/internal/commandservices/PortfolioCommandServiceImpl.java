package com.cashi.systemconfiguration.application.internal.commandservices;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.TenantRepository;
import com.cashi.systemconfiguration.domain.model.commands.UpdatePortfolioCommand;
import com.cashi.systemconfiguration.domain.services.PortfolioCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioCommandServiceImpl implements PortfolioCommandService {

    private final PortfolioRepository portfolioRepository;
    private final TenantRepository tenantRepository;

    public PortfolioCommandServiceImpl(PortfolioRepository portfolioRepository,
                                      TenantRepository tenantRepository) {
        this.portfolioRepository = portfolioRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional
    public Portfolio createPortfolio(Integer tenantId, String portfolioCode, String portfolioName,
                                    String description) {
        // Validar que el tenant existe
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado con ID: " + tenantId));

        // Validar que el código de portfolio no exista para este tenant
        if (portfolioRepository.existsByTenantAndPortfolioCode(tenant, portfolioCode)) {
            throw new IllegalArgumentException("Ya existe un portfolio con el código: " + portfolioCode + " para este cliente");
        }

        // Crear el portfolio
        Portfolio portfolio = new Portfolio(tenant, portfolioCode, portfolioName);

        // Establecer descripción si existe
        if (description != null && !description.isBlank()) {
            portfolio.setDescription(description);
        }

        // Guardar y retornar
        return portfolioRepository.save(portfolio);
    }

    @Override
    @Transactional
    public Portfolio updatePortfolio(Integer portfolioId, UpdatePortfolioCommand command) {
        // Buscar el portfolio
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio no encontrado con ID: " + portfolioId));

        // Actualizar campos si están presentes
        if (command.portfolioName() != null && !command.portfolioName().isBlank()) {
            portfolio.setPortfolioName(command.portfolioName());
        }

        if (command.description() != null) {
            portfolio.setDescription(command.description());
        }

        if (command.isActive() != null) {
            portfolio.setIsActive(command.isActive() ? 1 : 0);
        }

        // Guardar y retornar
        return portfolioRepository.save(portfolio);
    }

    @Override
    @Transactional
    public void deletePortfolio(Integer portfolioId) {
        // Buscar el portfolio
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio no encontrado con ID: " + portfolioId));

        // Delete portfolio
        portfolioRepository.delete(portfolio);
    }
}
