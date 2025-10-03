package com.cashi.paymentprocessing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResource(
        Long id,
        Integer installmentNumber,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate paidDate,
        String status,
        String statusDescription
) {
}
