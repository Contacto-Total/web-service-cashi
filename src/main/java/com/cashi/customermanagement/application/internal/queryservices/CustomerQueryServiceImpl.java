package com.cashi.customermanagement.application.internal.queryservices;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomerQueryServiceImpl {

    private final CustomerRepository customerRepository;

    public CustomerQueryServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<Customer> getCustomerById(String customerId) {
        return customerRepository.findByIdentificationCode(customerId);
    }

    public Optional<Customer> getCustomerByDocumentNumber(String documentNumber) {
        return customerRepository.findByDocument(documentNumber);
    }

    public List<Customer> searchCustomers(String query) {
        return customerRepository.findByFullNameContainingIgnoreCase(query);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllWithContactMethods();
    }

    /**
     * Busca un cliente por teléfono dentro de un tenant, portfolio y subportfolio específico
     * @param phoneNumber Número de teléfono a buscar
     * @param tenantId ID del inquilino
     * @param portfolioId ID de la cartera
     * @param subPortfolioId ID de la subcartera
     * @return Optional con el cliente encontrado
     */
    public Optional<Customer> getCustomerByPhone(String phoneNumber, Long tenantId, Long portfolioId, Long subPortfolioId) {
        return customerRepository.findByPhoneAndTenantAndPortfolio(phoneNumber, tenantId, portfolioId, subPortfolioId);
    }
}
