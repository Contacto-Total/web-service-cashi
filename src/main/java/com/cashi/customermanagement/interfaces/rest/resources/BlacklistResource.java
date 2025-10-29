package com.cashi.customermanagement.interfaces.rest.resources;

import jakarta.validation.constraints.Future;

import java.time.LocalDate;

public record BlacklistResource(
        Long id,
        Long customerId,
        Long tenantId,
        String tenantName,
        Long portfolioId,
        String portfolioName,
        Long subPortfolioId,
        String subPortfolioName,
        String document,
        String email,
        String phone,
        LocalDate startDate,
        @Future(message = "La fecha de fin debe ser mayor al día actual")
        LocalDate endDate
) {
}
