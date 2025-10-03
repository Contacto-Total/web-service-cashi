package com.cashi.systemconfiguration.application.internal.commandservices;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.TenantRepository;
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
    public Portfolio createPortfolio(Long tenantId, String portfolioCode, String portfolioName,
                                    String portfolioType, Long parentPortfolioId, String description) {
        // Validar que el tenant existe
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado con ID: " + tenantId));

        // Validar que el código de portfolio no exista para este tenant
        if (portfolioRepository.existsByTenantAndPortfolioCode(tenant, portfolioCode)) {
            throw new IllegalArgumentException("Ya existe un portfolio con el código: " + portfolioCode + " para este cliente");
        }

        // Buscar portfolio padre si se especifica
        Portfolio parentPortfolio = null;
        if (parentPortfolioId != null) {
            parentPortfolio = portfolioRepository.findById(parentPortfolioId)
                    .orElseThrow(() -> new IllegalArgumentException("Portfolio padre no encontrado con ID: " + parentPortfolioId));

            // Validar que el portfolio padre pertenece al mismo tenant
            if (!parentPortfolio.getTenant().getId().equals(tenantId)) {
                throw new IllegalArgumentException("El portfolio padre debe pertenecer al mismo cliente");
            }
        }

        // Convertir tipo de portfolio de String a Enum
        Portfolio.PortfolioType type = null;
        if (portfolioType != null && !portfolioType.isBlank()) {
            try {
                type = Portfolio.PortfolioType.valueOf(portfolioType);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo de portfolio inválido: " + portfolioType);
            }
        }

        // Crear el portfolio
        Portfolio portfolio;
        if (parentPortfolio != null) {
            portfolio = new Portfolio(tenant, portfolioCode, portfolioName, type, parentPortfolio);
        } else {
            portfolio = new Portfolio(tenant, portfolioCode, portfolioName, type);
        }

        // Establecer descripción si existe
        if (description != null && !description.isBlank()) {
            portfolio.setDescription(description);
        }

        // Guardar y retornar
        return portfolioRepository.save(portfolio);
    }
}
