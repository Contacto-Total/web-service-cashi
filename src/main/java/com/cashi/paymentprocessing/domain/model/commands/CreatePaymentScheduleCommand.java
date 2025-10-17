package com.cashi.paymentprocessing.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePaymentScheduleCommand(
    String customerId,
    String managementId,
    String scheduleType,
    BigDecimal negotiatedAmount,
    List<InstallmentData> installments
) {
    public record InstallmentData(
        Integer installmentNumber,
        BigDecimal amount,
        LocalDate dueDate
    ) {}
}
