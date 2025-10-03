package com.cashi.paymentprocessing.domain.model.entities;

import com.cashi.paymentprocessing.domain.model.valueobjects.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "installments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer installmentNumber;

    private BigDecimal amount;

    private LocalDate dueDate;

    private LocalDate paidDate;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "status", column = @Column(name = "installment_status")),
        @AttributeOverride(name = "description", column = @Column(name = "installment_status_description"))
    })
    private PaymentStatus status;

    public Installment(Integer installmentNumber, BigDecimal amount, LocalDate dueDate) {
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = PaymentStatus.pending();
    }

    public void markAsPaid(LocalDate paidDate) {
        this.paidDate = paidDate;
        this.status = PaymentStatus.completed();
    }

    public void cancel() {
        this.status = PaymentStatus.cancelled();
    }

    public boolean isPending() {
        return status != null && status.isPending();
    }

    public boolean isOverdue() {
        return isPending() && dueDate != null && dueDate.isBefore(LocalDate.now());
    }
}
