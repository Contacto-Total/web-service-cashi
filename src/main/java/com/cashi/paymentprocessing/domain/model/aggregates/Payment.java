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
@Table(name = "pagos")
@Getter
@NoArgsConstructor
public class Payment extends AggregateRoot {

    @Embedded
    private PaymentId paymentId;

    @Column(name = "id_cliente")
    private String customerId;

    @Column(name = "id_gestion")
    private String managementId;

    @Column(name = "monto")
    private BigDecimal amount;

    @Column(name = "fecha_pago")
    private LocalDate paymentDate;

    @Column(name = "metodo_pago")
    private String paymentMethod;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "status", column = @Column(name = "estado_pago")),
        @AttributeOverride(name = "description", column = @Column(name = "descripcion_estado_pago"))
    })
    private PaymentStatus status;

    @Embedded
    private TransactionId transactionId;

    @Column(name = "numero_voucher")
    private String voucherNumber;

    @Column(name = "nombre_banco")
    private String bankName;

    @Column(name = "confirmado_en")
    private LocalDateTime confirmedAt;

    @Column(name = "notas", length = 1000)
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
