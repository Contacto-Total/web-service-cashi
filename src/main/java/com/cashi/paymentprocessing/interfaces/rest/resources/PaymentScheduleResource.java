package com.cashi.paymentprocessing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentScheduleResource(
        Long id,
        String scheduleId,
        String customerId,
        String managementId,
        BigDecimal totalAmount,
        Integer numberOfInstallments,
        LocalDate startDate,
        Boolean isActive,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        Integer paidInstallments,
        Integer pendingInstallments,
        List<InstallmentResource> installments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
