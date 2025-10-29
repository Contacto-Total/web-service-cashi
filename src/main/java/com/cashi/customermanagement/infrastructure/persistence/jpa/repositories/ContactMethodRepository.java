package com.cashi.customermanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.customermanagement.domain.model.entities.ContactMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactMethodRepository extends JpaRepository<ContactMethod, Long> {

    /**
     * Encuentra todos los métodos de contacto de un cliente
     */
    List<ContactMethod> findByCustomerId(Long customerId);

    /**
     * Encuentra métodos de contacto por cliente y tipo
     */
    List<ContactMethod> findByCustomerIdAndContactType(Long customerId, String contactType);

    /**
     * Encuentra métodos de contacto por cliente y subtipo específico
     */
    List<ContactMethod> findByCustomerIdAndSubtype(Long customerId, String subtype);

    /**
     * Busca un método de contacto por tenant, subtipo y valor, cargando el cliente con métodos de contacto
     */
    @Query("SELECT cm FROM ContactMethod cm " +
           "LEFT JOIN FETCH cm.customer c " +
           "LEFT JOIN FETCH c.contactMethods " +
           "WHERE c.tenantId = :tenantId " +
           "AND cm.subtype = :subtype " +
           "AND cm.value = :value")
    Optional<ContactMethod> findByTenantIdAndSubtypeAndValueWithCustomer(
        @Param("tenantId") Long tenantId,
        @Param("subtype") String subtype,
        @Param("value") String value
    );

    /**
     * Busca todos los métodos de contacto por tenant, subtipo y valor (para búsqueda en múltiples subcarteras)
     */
    @Query("SELECT DISTINCT cm FROM ContactMethod cm " +
           "LEFT JOIN FETCH cm.customer c " +
           "LEFT JOIN FETCH c.contactMethods " +
           "WHERE c.tenantId = :tenantId " +
           "AND cm.subtype = :subtype " +
           "AND cm.value = :value")
    List<ContactMethod> findAllByTenantIdAndSubtypeAndValueWithCustomer(
        @Param("tenantId") Long tenantId,
        @Param("subtype") String subtype,
        @Param("value") String value
    );

    /**
     * Busca métodos de contacto por subtipo y valor SIN filtrar por tenant (búsqueda multi-tenant)
     */
    @Query("SELECT DISTINCT cm FROM ContactMethod cm " +
           "LEFT JOIN FETCH cm.customer c " +
           "LEFT JOIN FETCH c.contactMethods " +
           "WHERE cm.subtype = :subtype " +
           "AND cm.value = :value")
    List<ContactMethod> findAllBySubtypeAndValueWithCustomer(
        @Param("subtype") String subtype,
        @Param("value") String value
    );

    /**
     * Busca métodos de contacto por tipo (telefono/email) y valor, filtrando por tenant
     * Busca en TODOS los métodos de contacto de ese tipo (ej: todos los teléfonos sin importar si es principal, secundario, etc.)
     */
    @Query("SELECT DISTINCT cm FROM ContactMethod cm " +
           "LEFT JOIN FETCH cm.customer c " +
           "LEFT JOIN FETCH c.contactMethods " +
           "WHERE c.tenantId = :tenantId " +
           "AND cm.contactType = :contactType " +
           "AND cm.value = :value")
    List<ContactMethod> findAllByTenantIdAndContactTypeAndValueWithCustomer(
        @Param("tenantId") Long tenantId,
        @Param("contactType") String contactType,
        @Param("value") String value
    );

    /**
     * Busca métodos de contacto por tipo (telefono/email) y valor SIN filtrar por tenant (búsqueda multi-tenant)
     * Busca en TODOS los métodos de contacto de ese tipo
     */
    @Query("SELECT DISTINCT cm FROM ContactMethod cm " +
           "LEFT JOIN FETCH cm.customer c " +
           "LEFT JOIN FETCH c.contactMethods " +
           "WHERE cm.contactType = :contactType " +
           "AND cm.value = :value")
    List<ContactMethod> findAllByContactTypeAndValueWithCustomer(
        @Param("contactType") String contactType,
        @Param("value") String value
    );
}
