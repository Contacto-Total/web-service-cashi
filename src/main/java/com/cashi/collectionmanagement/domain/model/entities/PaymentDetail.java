package com.cashi.collectionmanagement.domain.model.entities;

import com.cashi.collectionmanagement.domain.model.valueobjects.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "detalles_pago")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monto")
    private BigDecimal amount;

    @Column(name = "fecha_programada")
    private LocalDate scheduledDate;

    @Embedded
    private PaymentMethod paymentMethod;

    @Column(name = "numero_voucher")
    private String voucherNumber;

    @Column(name = "nombre_banco")
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
