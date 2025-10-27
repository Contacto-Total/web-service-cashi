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
}
