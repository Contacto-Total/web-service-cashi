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

        // Usar los nombres directamente desde la entidad Customer
        // Ya no necesitamos hacer consultas adicionales porque ahora están guardados directamente
        String subPortfolioCode = null;

        // Solo buscar el código si tenemos el ID y aún no está en la entidad
        if (entity.getSubPortfolioId() != null) {
            var subPortfolioOpt = subPortfolioRepository.findById(entity.getSubPortfolioId().intValue());
            if (subPortfolioOpt.isPresent()) {
                subPortfolioCode = subPortfolioOpt.get().getSubPortfolioCode();
            }
        }

        return new CustomerResource(
                entity.getId(),
                entity.getCustomerId(),
                entity.getIdentificationCode(),
                entity.getAccountNumber(),
                // Información financiera/deuda
                entity.getOverdueDays(),
                entity.getOverdueAmount(),
                entity.getPrincipalAmount(),
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
                // Información de jerarquía multi-tenant
                entity.getTenantId(),
                entity.getTenantName(),
                entity.getPortfolioId(),
                entity.getPortfolioName(),
                entity.getSubPortfolioId(),
                entity.getSubPortfolioName(),
                subPortfolioCode
        );
    }
}
