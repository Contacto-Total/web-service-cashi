package com.cashi.paymentprocessing.domain.services;

import com.cashi.paymentprocessing.domain.model.aggregates.Payment;
import com.cashi.paymentprocessing.domain.model.aggregates.PaymentSchedule;
import com.cashi.paymentprocessing.domain.model.commands.*;

public interface PaymentCommandService {

    Payment handle(CreatePaymentCommand command);

    Payment handle(ConfirmPaymentCommand command);

    Payment handle(CancelPaymentCommand command);

    PaymentSchedule handle(CreatePaymentScheduleCommand command);

    PaymentSchedule handle(RecordInstallmentPaymentCommand command);

    PaymentSchedule handle(CancelPaymentScheduleCommand command);
}
