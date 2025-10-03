package com.cashi.paymentprocessing.application.internal.queryservices;

import com.cashi.paymentprocessing.domain.model.aggregates.Payment;
import com.cashi.paymentprocessing.domain.model.aggregates.PaymentSchedule;
import com.cashi.paymentprocessing.domain.model.queries.*;
import com.cashi.paymentprocessing.domain.services.PaymentQueryService;
import com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories.PaymentRepository;
import com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories.PaymentScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentQueryServiceImpl implements PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentScheduleRepository scheduleRepository;

    public PaymentQueryServiceImpl(PaymentRepository paymentRepository, PaymentScheduleRepository scheduleRepository) {
        this.paymentRepository = paymentRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public Optional<Payment> handle(GetPaymentByIdQuery query) {
        return paymentRepository.findByPaymentId_PaymentId(query.paymentId());
    }

    @Override
    public List<Payment> handle(GetPaymentsByCustomerQuery query) {
        return paymentRepository.findByCustomerId(query.customerId());
    }

    @Override
    public List<Payment> handle(GetPendingPaymentsQuery query) {
        return paymentRepository.findByCustomerIdAndStatus_Status(query.customerId(), "PENDING");
    }

    @Override
    public Optional<PaymentSchedule> handle(GetPaymentScheduleByIdQuery query) {
        return scheduleRepository.findByScheduleId_ScheduleId(query.scheduleId());
    }

    @Override
    public List<PaymentSchedule> handle(GetPaymentSchedulesByCustomerQuery query) {
        return scheduleRepository.findByCustomerId(query.customerId());
    }
}
