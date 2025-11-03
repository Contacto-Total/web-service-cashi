package com.cashi.customermanagement.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.List;

public record CustomerResource(
        Long id,
        String customerId,
        String identificationCode,
        String accountNumber,
        // Información financiera/deuda
        Integer overdueDays,
        Double overdueAmount,
        Double principalAmount,
        String documentNumber,
        String fullName,
        String documentType,
        LocalDate birthDate,
        Integer age,
        // Nombres
        String firstName,
        String secondName,
        String firstLastName,
        String secondLastName,
        // Datos personales
        String maritalStatus,
        String occupation,
        String customerType,
        // Ubicación
        String address,
        String district,
        String province,
        String department,
        // Referencias
        String personalReference,
        // Estado
        String status,
        LocalDate importDate,
        // Métodos de contacto
        List<ContactMethodResource> contactMethods,
        // Información de jerarquía multi-tenant
        Long tenantId,
        String tenantName,
        Long portfolioId,
        String portfolioName,
        Long subPortfolioId,
        String subPortfolioName,
        String subPortfolioCode
) {
}
