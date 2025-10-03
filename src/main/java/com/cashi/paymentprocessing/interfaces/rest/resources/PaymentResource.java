package com.cashi.paymentprocessing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResource(
        Long id,
        String paymentId,
        String customerId,
        String managementId,
        BigDecimal amount,
        LocalDate paymentDate,
        String paymentMethod,
        String status,
        String statusDescription,
        String transactionId,
        String voucherNumber,
        String bankName,
        LocalDateTime confirmedAt,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
