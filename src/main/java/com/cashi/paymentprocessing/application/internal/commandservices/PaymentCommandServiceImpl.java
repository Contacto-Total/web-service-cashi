package com.cashi.paymentprocessing.application.internal.commandservices;

import com.cashi.paymentprocessing.domain.model.aggregates.Payment;
import com.cashi.paymentprocessing.domain.model.aggregates.PaymentSchedule;
import com.cashi.paymentprocessing.domain.model.commands.*;
import com.cashi.paymentprocessing.domain.model.valueobjects.TransactionId;
import com.cashi.paymentprocessing.domain.services.PaymentCommandService;
import com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories.PaymentRepository;
import com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories.PaymentScheduleRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentCommandServiceImpl implements PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final PaymentScheduleRepository scheduleRepository;

    public PaymentCommandServiceImpl(PaymentRepository paymentRepository, PaymentScheduleRepository scheduleRepository) {
        this.paymentRepository = paymentRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public Payment handle(CreatePaymentCommand command) {
        var payment = new Payment(
            command.customerId(),
            command.managementId(),
            command.amount(),
            command.paymentDate(),
            command.paymentMethod()
        );

        if (command.voucherNumber() != null) {
            payment.setVoucherDetails(command.voucherNumber(), command.bankName());
        }

        if (command.notes() != null) {
            payment.addNotes(command.notes());
        }

        return paymentRepository.save(payment);
    }

    @Override
    public Payment handle(ConfirmPaymentCommand command) {
        var payment = paymentRepository.findByPaymentId_PaymentId(command.paymentId())
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + command.paymentId()));

        var transactionId = new TransactionId(command.transactionId());
        payment.confirm(transactionId);

        return paymentRepository.save(payment);
    }

    @Override
    public Payment handle(CancelPaymentCommand command) {
        var payment = paymentRepository.findByPaymentId_PaymentId(command.paymentId())
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + command.paymentId()));

        payment.cancel();

        return paymentRepository.save(payment);
    }

    @Override
    public PaymentSchedule handle(CreatePaymentScheduleCommand command) {
        var schedule = new PaymentSchedule(
            command.customerId(),
            command.managementId(),
            command.totalAmount(),
            command.numberOfInstallments(),
            command.startDate()
        );

        return scheduleRepository.save(schedule);
    }

    @Override
    public PaymentSchedule handle(RecordInstallmentPaymentCommand command) {
        var schedule = scheduleRepository.findByScheduleId_ScheduleId(command.scheduleId())
            .orElseThrow(() -> new IllegalArgumentException("Payment schedule not found: " + command.scheduleId()));

        schedule.markInstallmentAsPaid(command.installmentNumber(), command.paidDate());

        return scheduleRepository.save(schedule);
    }

    @Override
    public PaymentSchedule handle(CancelPaymentScheduleCommand command) {
        var schedule = scheduleRepository.findByScheduleId_ScheduleId(command.scheduleId())
            .orElseThrow(() -> new IllegalArgumentException("Payment schedule not found: " + command.scheduleId()));

        schedule.cancel();

        return scheduleRepository.save(schedule);
    }
}
