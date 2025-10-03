package com.cashi.paymentprocessing.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentCommand(
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
