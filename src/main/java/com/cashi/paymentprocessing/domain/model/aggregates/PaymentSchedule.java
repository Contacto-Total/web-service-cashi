package com.cashi.paymentprocessing.domain.model.aggregates;

import com.cashi.paymentprocessing.domain.model.entities.Installment;
import com.cashi.paymentprocessing.domain.model.valueobjects.PaymentScheduleId;
import com.cashi.shared.domain.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_schedules")
@Getter
@NoArgsConstructor
public class PaymentSchedule extends AggregateRoot {

    @Embedded
    private PaymentScheduleId scheduleId;

    private String customerId;

    private String managementId;

    private BigDecimal totalAmount;

    private Integer numberOfInstallments;

    private LocalDate startDate;

    private Boolean isActive;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "payment_schedule_id")
    private List<Installment> installments = new ArrayList<>();

    public PaymentSchedule(String customerId, String managementId, BigDecimal totalAmount, Integer numberOfInstallments, LocalDate startDate) {
        this.scheduleId = PaymentScheduleId.generate();
        this.customerId = customerId;
        this.managementId = managementId;
        this.totalAmount = totalAmount;
        this.numberOfInstallments = numberOfInstallments;
        this.startDate = startDate;
        this.isActive = true;
        generateInstallments();
    }

    private void generateInstallments() {
        BigDecimal installmentAmount = totalAmount.divide(BigDecimal.valueOf(numberOfInstallments), 2, BigDecimal.ROUND_HALF_UP);

        for (int i = 1; i <= numberOfInstallments; i++) {
            LocalDate dueDate = startDate.plusMonths(i - 1);
            installments.add(new Installment(i, installmentAmount, dueDate));
        }
    }

    public void markInstallmentAsPaid(Integer installmentNumber, LocalDate paidDate) {
        installments.stream()
                .filter(inst -> inst.getInstallmentNumber().equals(installmentNumber))
                .findFirst()
                .ifPresent(inst -> inst.markAsPaid(paidDate));
    }

    public void cancel() {
        this.isActive = false;
        installments.forEach(Installment::cancel);
    }

    public BigDecimal getPaidAmount() {
        return installments.stream()
                .filter(inst -> inst.getStatus().isCompleted())
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPendingAmount() {
        return totalAmount.subtract(getPaidAmount());
    }

    public Integer getPaidInstallments() {
        return (int) installments.stream()
                .filter(inst -> inst.getStatus().isCompleted())
                .count();
    }

    public Integer getPendingInstallments() {
        return numberOfInstallments - getPaidInstallments();
    }

    public boolean isFullyPaid() {
        return getPaidInstallments().equals(numberOfInstallments);
    }
}
