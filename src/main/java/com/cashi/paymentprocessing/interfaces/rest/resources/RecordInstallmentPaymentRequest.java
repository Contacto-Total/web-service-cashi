package com.cashi.paymentprocessing.interfaces.rest.resources;

import java.time.LocalDate;

public record RecordInstallmentPaymentRequest(
        Integer installmentNumber,
        LocalDate paidDate
) {
}
