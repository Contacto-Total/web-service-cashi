package com.cashi.customermanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerId(String customerId);
    Optional<Customer> findByDocumentNumberNumber(String documentNumber);
    Optional<Customer> findByDocumentCode(String documentCode);
    List<Customer> findByFullNameContainingIgnoreCase(String name);
    List<Customer> findByTenantId(Long tenantId);
    Optional<Customer> findByTenantIdAndDocumentCode(Long tenantId, String documentCode);
}
