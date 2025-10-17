package com.cashi.paymentprocessing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InstallmentStatusHistoryResource(
    Long id,
    Long installmentId,
    String managementId,
    String status,
    String statusDescription,
    LocalDateTime changeDate,
    LocalDateTime actualPaymentDate,
    BigDecimal amountPaid,
    String observations,
    String registeredBy
) {
}
