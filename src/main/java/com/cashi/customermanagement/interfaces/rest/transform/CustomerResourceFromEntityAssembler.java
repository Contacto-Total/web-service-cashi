package com.cashi.customermanagement.interfaces.rest.transform;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import com.cashi.customermanagement.domain.model.entities.ContactMethod;
import com.cashi.customermanagement.interfaces.rest.resources.*;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomerResourceFromEntityAssembler {

    private final SubPortfolioRepository subPortfolioRepository;

    public CustomerResourceFromEntityAssembler(SubPortfolioRepository subPortfolioRepository) {
        this.subPortfolioRepository = subPortfolioRepository;
    }

    public CustomerResource toResourceFromEntity(Customer entity) {
        // Mapear métodos de contacto
        List<ContactMethodResource> contactMethods = entity.getContactMethods().stream()
                .map(cm -> new ContactMethodResource(
                        cm.getId(),
                        cm.getContactType(),
                        cm.getSubtype(),
                        cm.getValue(),
                        cm.getLabel(),
                        cm.getImportDate(),
                        cm.getStatus()
                ))
                .collect(Collectors.toList());

        // Buscar información de subcartera si existe
        Long subPortfolioId = entity.getSubPortfolioId();
        String subPortfolioName = null;
        String subPortfolioCode = null;
        String portfolioName = null;
        String tenantName = null;

        if (subPortfolioId != null) {
            // Buscar la información de la subcartera
            var subPortfolioOpt = subPortfolioRepository.findById(subPortfolioId.intValue());
            if (subPortfolioOpt.isPresent()) {
                SubPortfolio subPortfolio = subPortfolioOpt.get();
                subPortfolioName = subPortfolio.getSubPortfolioName();
                subPortfolioCode = subPortfolio.getSubPortfolioCode();
                if (subPortfolio.getPortfolio() != null) {
                    portfolioName = subPortfolio.getPortfolio().getPortfolioName();
                    if (subPortfolio.getPortfolio().getTenant() != null) {
                        tenantName = subPortfolio.getPortfolio().getTenant().getTenantName();
                    }
                }
            }
        }

        return new CustomerResource(
                entity.getId(),
                entity.getCustomerId(),
                entity.getIdentificationCode(),
                entity.getDocument(),
                entity.getFullName(),
                "DNI", // default document type
                entity.getBirthDate(),
                entity.getAge(),
                // Nombres
                entity.getFirstName(),
                entity.getSecondName(),
                entity.getFirstLastName(),
                entity.getSecondLastName(),
                // Datos personales
                entity.getMaritalStatus(),
                entity.getOccupation(),
                entity.getCustomerType(),
                // Ubicación
                entity.getAddress(),
                entity.getDistrict(),
                entity.getProvince(),
                entity.getDepartment(),
                // Referencias
                entity.getPersonalReference(),
                // Estado
                entity.getStatus(),
                entity.getImportDate(),
                // Métodos de contacto
                contactMethods,
                // Información de subcartera
                subPortfolioId,
                subPortfolioName,
                subPortfolioCode,
                portfolioName,
                tenantName
        );
    }
}
