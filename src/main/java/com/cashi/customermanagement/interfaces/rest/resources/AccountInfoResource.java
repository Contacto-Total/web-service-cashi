package com.cashi.customermanagement.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountInfoResource(
        String accountNumber,
        String productType,
        LocalDate disbursementDate,
        BigDecimal originalAmount,
        Integer termMonths,
        BigDecimal interestRate
) {
}
