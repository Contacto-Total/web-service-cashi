package com.cashi.paymentprocessing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePaymentScheduleRequest(
        String customerId,
        String managementId,
        String scheduleType, // "FINANCIERA" o "CONFIANZA"
        BigDecimal negotiatedAmount, // Para tipo FINANCIERA
        List<InstallmentRequest> installments // Cuotas ingresadas manualmente por el usuario
) {
    public record InstallmentRequest(
            Integer installmentNumber,
            BigDecimal amount,
            LocalDate dueDate
    ) {}
}
