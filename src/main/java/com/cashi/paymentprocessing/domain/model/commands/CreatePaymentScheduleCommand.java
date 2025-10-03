package com.cashi.paymentprocessing.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentScheduleCommand(
    String customerId,
    String managementId,
    BigDecimal totalAmount,
    Integer numberOfInstallments,
    LocalDate startDate
) {
}
