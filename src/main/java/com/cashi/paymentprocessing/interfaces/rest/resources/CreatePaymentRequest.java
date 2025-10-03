package com.cashi.paymentprocessing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentRequest(
        String customerId,
        String managementId,
        BigDecimal amount,
        LocalDate paymentDate,
        String paymentMethod,
        String voucherNumber,
        String bankName,
        String notes
) {
}
