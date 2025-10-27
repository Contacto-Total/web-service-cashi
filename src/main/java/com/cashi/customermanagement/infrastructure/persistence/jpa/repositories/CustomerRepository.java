package com.cashi.customermanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByIdentificationCode(String identificationCode);
    Optional<Customer> findByDocument(String document);
    List<Customer> findByFullNameContainingIgnoreCase(String name);
    List<Customer> findByTenantId(Long tenantId);
    Optional<Customer> findByTenantIdAndIdentificationCode(Long tenantId, String identificationCode);
    Optional<Customer> findByTenantIdAndDocument(Long tenantId, String document);

    // Get top 5 most recent customers
    List<Customer> findTop5ByOrderByIdDesc();

    // Métodos con JOIN FETCH para cargar métodos de contacto (retornan único resultado)
    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.contactMethods WHERE c.tenantId = :tenantId AND c.identificationCode = :identificationCode")
    Optional<Customer> findByTenantIdAndIdentificationCodeWithContactMethods(@Param("tenantId") Long tenantId, @Param("identificationCode") String identificationCode);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.contactMethods WHERE c.tenantId = :tenantId AND c.document = :document")
    Optional<Customer> findByTenantIdAndDocumentWithContactMethods(@Param("tenantId") Long tenantId, @Param("document") String document);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.contactMethods WHERE c.tenantId = :tenantId AND c.accountNumber = :accountNumber")
    Optional<Customer> findByTenantIdAndAccountNumberWithContactMethods(@Param("tenantId") Long tenantId, @Param("accountNumber") String accountNumber);

    // Métodos con JOIN FETCH que retornan múltiples resultados (para búsqueda en múltiples subcarteras)
    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.contactMethods WHERE c.tenantId = :tenantId AND c.identificationCode = :identificationCode")
    List<Customer> findAllByTenantIdAndIdentificationCodeWithContactMethods(@Param("tenantId") Long tenantId, @Param("identificationCode") String identificationCode);

    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.contactMethods WHERE c.tenantId = :tenantId AND c.document = :document")
    List<Customer> findAllByTenantIdAndDocumentWithContactMethods(@Param("tenantId") Long tenantId, @Param("document") String document);

    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.contactMethods WHERE c.tenantId = :tenantId AND c.accountNumber = :accountNumber")
    List<Customer> findAllByTenantIdAndAccountNumberWithContactMethods(@Param("tenantId") Long tenantId, @Param("accountNumber") String accountNumber);

    // Method to fetch all customers with their contact methods
    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.contactMethods")
    List<Customer> findAllWithContactMethods();
}
