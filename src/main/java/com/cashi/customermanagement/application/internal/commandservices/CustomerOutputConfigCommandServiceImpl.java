package com.cashi.customermanagement.application.internal.commandservices;

import com.cashi.customermanagement.domain.model.aggregates.CustomerOutputConfig;
import com.cashi.customermanagement.domain.model.commands.SaveCustomerOutputConfigCommand;
import com.cashi.customermanagement.domain.services.CustomerOutputConfigCommandService;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerOutputConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementación del servicio de comandos para CustomerOutputConfig
 */
@Service
@RequiredArgsConstructor
public class CustomerOutputConfigCommandServiceImpl implements CustomerOutputConfigCommandService {

    private final CustomerOutputConfigRepository repository;

    /**
     * Guarda o actualiza configuración de outputs
     *
     * LÓGICA:
     * 1. Busca si existe configuración para tenant+portfolio
     * 2. Si existe → actualiza fieldsConfig
     * 3. Si no existe → crea nueva configuración
     * 4. Guarda en BD
     *
     * LOGS:
     * - Muestra si está creando o actualizando
     * - Muestra tenant y portfolio afectados
     * - Muestra primeros 100 caracteres del JSON config
     */
    @Override
    @Transactional
    public CustomerOutputConfig handle(SaveCustomerOutputConfigCommand command) {
        System.out.println("💾 Guardando configuración de outputs del cliente:");
        System.out.println("   → Tenant ID: " + command.tenantId());
        System.out.println("   → Portfolio ID: " + (command.portfolioId() != null ? command.portfolioId() : "General (todas las carteras)"));
        System.out.println("   → Config preview: " +
            (command.fieldsConfig().length() > 100
                ? command.fieldsConfig().substring(0, 100) + "..."
                : command.fieldsConfig()));

        // Buscar configuración existente
        Optional<CustomerOutputConfig> existingConfig;
        if (command.portfolioId() != null) {
            existingConfig = repository.findByTenantIdAndPortfolioId(command.tenantId(), command.portfolioId());
        } else {
            existingConfig = repository.findByTenantIdAndPortfolioIdIsNull(command.tenantId());
        }

        CustomerOutputConfig config;
        if (existingConfig.isPresent()) {
            // Actualizar existente
            config = existingConfig.get();
            config.updateFieldsConfig(command.fieldsConfig());
            System.out.println("   ✏️ Actualizando configuración existente (ID: " + config.getId() + ")");
        } else {
            // Crear nueva
            config = new CustomerOutputConfig(
                command.tenantId(),
                command.portfolioId(),
                command.fieldsConfig()
            );
            System.out.println("   ➕ Creando nueva configuración");
        }

        CustomerOutputConfig saved = repository.save(config);
        System.out.println("   ✅ Configuración guardada exitosamente (ID: " + saved.getId() + ")");

        return saved;
    }
}
