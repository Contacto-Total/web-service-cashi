package com.cashi.customermanagement.interfaces.rest.resources;

import java.time.LocalDate;

public record CustomerResource(
        Long id,
        String customerId,
        String fullName,
        String documentType,
        String documentNumber,
        LocalDate birthDate,
        Integer age
) {
}
