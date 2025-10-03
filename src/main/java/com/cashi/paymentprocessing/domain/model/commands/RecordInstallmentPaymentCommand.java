package com.cashi.paymentprocessing.domain.model.commands;

import java.time.LocalDate;

public record RecordInstallmentPaymentCommand(
    String scheduleId,
    Integer installmentNumber,
    LocalDate paidDate
) {
}
