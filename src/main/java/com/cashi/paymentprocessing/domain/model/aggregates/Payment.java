package com.cashi.paymentprocessing.domain.model.aggregates;

import com.cashi.paymentprocessing.domain.model.valueobjects.PaymentId;
import com.cashi.paymentprocessing.domain.model.valueobjects.PaymentStatus;
import com.cashi.paymentprocessing.domain.model.valueobjects.TransactionId;
import com.cashi.shared.domain.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
public class Payment extends AggregateRoot {

    @Embedded
    private PaymentId paymentId;

    private String customerId;

    private String managementId;

    private BigDecimal amount;

    private LocalDate paymentDate;

    private String paymentMethod;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "status", column = @Column(name = "payment_status")),
        @AttributeOverride(name = "description", column = @Column(name = "payment_status_description"))
    })
    private PaymentStatus status;

    @Embedded
    private TransactionId transactionId;

    private String voucherNumber;

    private String bankName;

    private LocalDateTime confirmedAt;

    @Column(length = 1000)
    private String notes;

    public Payment(String customerId, String managementId, BigDecimal amount, LocalDate paymentDate, String paymentMethod) {
        this.paymentId = PaymentId.generate();
        this.customerId = customerId;
        this.managementId = managementId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.pending();
    }

    public void confirm(TransactionId transactionId) {
        this.status = PaymentStatus.completed();
        this.transactionId = transactionId;
        this.confirmedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = PaymentStatus.cancelled();
    }

    public void setVoucherDetails(String voucherNumber, String bankName) {
        this.voucherNumber = voucherNumber;
        this.bankName = bankName;
    }

    public void addNotes(String notes) {
        this.notes = notes;
    }

    public boolean isPending() {
        return status != null && status.isPending();
    }

    public boolean isCompleted() {
        return status != null && status.isCompleted();
    }
}
