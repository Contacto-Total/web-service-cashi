package com.cashi.systemconfiguration.application.internal.commandservices;

import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.TenantRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.systemconfiguration.domain.model.commands.CreateTenantCommand;
import com.cashi.systemconfiguration.domain.model.commands.UpdateTenantCommand;
import com.cashi.systemconfiguration.domain.services.TenantCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantCommandServiceImpl implements TenantCommandService {

    private final TenantRepository tenantRepository;
    private final PortfolioRepository portfolioRepository;

    public TenantCommandServiceImpl(TenantRepository tenantRepository, PortfolioRepository portfolioRepository) {
        this.tenantRepository = tenantRepository;
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    @Transactional
    public Tenant handle(CreateTenantCommand command) {
        // Validar que el código de tenant no exista
        if (tenantRepository.existsByTenantCode(command.tenantCode())) {
            throw new IllegalArgumentException("Ya existe un tenant con el código: " + command.tenantCode());
        }

        // Crear el tenant
        Tenant tenant = new Tenant(
            command.tenantCode(),
            command.tenantName(),
            command.businessName()
        );

        // Guardar y retornar
        return tenantRepository.save(tenant);
    }

    @Override
    @Transactional
    public Tenant handle(Integer tenantId, UpdateTenantCommand command) {
        // Buscar el tenant
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado con ID: " + tenantId));

        // Actualizar campos si están presentes
        if (command.tenantName() != null && !command.tenantName().isBlank()) {
            tenant.setTenantName(command.tenantName());
        }
        if (command.businessName() != null) {
            tenant.setBusinessName(command.businessName());
        }
        if (command.isActive() != null) {
            tenant.setIsActive(command.isActive() ? 1 : 0);
        }

        // Guardar y retornar
        return tenantRepository.save(tenant);
    }

    @Override
    @Transactional
    public void deleteTenant(Integer tenantId) {
        // Buscar el tenant
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado con ID: " + tenantId));

        // Verificar si tiene portfolios asociados
        long portfolioCount = portfolioRepository.countByTenant(tenant);
        if (portfolioCount > 0) {
            throw new IllegalStateException(
                "No se puede eliminar el tenant porque tiene " + portfolioCount + " cartera(s) asociada(s). " +
                "Elimine primero las carteras asociadas."
            );
        }

        // Hard delete - eliminar físicamente de la base de datos
        tenantRepository.delete(tenant);
    }
}
