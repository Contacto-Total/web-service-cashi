package com.cashi.collectionmanagement.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterPaymentRequest(
        BigDecimal amount,
        LocalDate scheduledDate,
        String paymentMethodType,
        String paymentMethodDetails,
        String voucherNumber,
        String bankName
) {
}
