package com.cashi.paymentprocessing.domain.model.entities;

import com.cashi.paymentprocessing.domain.model.valueobjects.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cuotas")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cuota")
    private Integer installmentNumber;

    @Column(name = "monto")
    private BigDecimal amount;

    @Column(name = "fecha_vencimiento")
    private LocalDate dueDate;

    @Column(name = "fecha_pago")
    private LocalDate paidDate;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "status", column = @Column(name = "estado_cuota")),
        @AttributeOverride(name = "description", column = @Column(name = "descripcion_estado_cuota"))
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
