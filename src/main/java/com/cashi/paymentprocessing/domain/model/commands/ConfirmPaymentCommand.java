package com.cashi.paymentprocessing.domain.model.commands;

public record ConfirmPaymentCommand(
    String paymentId,
    String transactionId
) {
}
