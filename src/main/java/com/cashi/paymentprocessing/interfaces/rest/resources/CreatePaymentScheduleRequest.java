package com.cashi.paymentprocessing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentScheduleRequest(
        String customerId,
        String managementId,
        BigDecimal totalAmount,
        Integer numberOfInstallments,
        LocalDate startDate
) {
}
