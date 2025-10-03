package com.cashi.collectionmanagement.domain.model.entities;

import com.cashi.collectionmanagement.domain.model.valueobjects.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payment_details")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    private LocalDate scheduledDate;

    @Embedded
    private PaymentMethod paymentMethod;

    private String voucherNumber;

    private String bankName;

    public PaymentDetail(BigDecimal amount, LocalDate scheduledDate, PaymentMethod paymentMethod) {
        this.amount = amount;
        this.scheduledDate = scheduledDate;
        this.paymentMethod = paymentMethod;
    }

    public void setVoucherDetails(String voucherNumber, String bankName) {
        this.voucherNumber = voucherNumber;
        this.bankName = bankName;
    }
}
