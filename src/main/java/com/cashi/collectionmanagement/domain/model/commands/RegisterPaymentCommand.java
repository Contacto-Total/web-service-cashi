package com.cashi.collectionmanagement.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterPaymentCommand(
    String managementId,
    BigDecimal amount,
    LocalDate scheduledDate,
    String paymentMethodType,
    String paymentMethodDetails,
    String voucherNumber,
    String bankName
) {
}
