package com.cashi.paymentprocessing.domain.services;

import com.cashi.paymentprocessing.domain.model.aggregates.Payment;
import com.cashi.paymentprocessing.domain.model.aggregates.PaymentSchedule;
import com.cashi.paymentprocessing.domain.model.queries.*;

import java.util.List;
import java.util.Optional;

public interface PaymentQueryService {

    Optional<Payment> handle(GetPaymentByIdQuery query);

    List<Payment> handle(GetPaymentsByCustomerQuery query);

    List<Payment> handle(GetPendingPaymentsQuery query);

    Optional<PaymentSchedule> handle(GetPaymentScheduleByIdQuery query);

    List<PaymentSchedule> handle(GetPaymentSchedulesByCustomerQuery query);
}
