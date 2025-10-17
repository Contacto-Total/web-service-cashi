package com.cashi.customermanagement.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "informacion_deuda")
@Getter
@NoArgsConstructor
public class DebtInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saldo_capital", precision = 10, scale = 2)
    private BigDecimal capitalBalance;

    @Column(name = "interes_vencido", precision = 10, scale = 2)
    private BigDecimal overdueInterest;

    @Column(name = "moras_acumuladas", precision = 10, scale = 2)
    private BigDecimal accumulatedLateFees;

    @Column(name = "gastos_cobranza", precision = 10, scale = 2)
    private BigDecimal collectionFees;

    @Column(name = "deuda_actual", precision = 15, scale = 2)
    private BigDecimal currentDebt;

    @Column(name = "saldo_total", precision = 15, scale = 2)
    private BigDecimal totalBalance;

    @Column(name = "dias_mora")
    private Integer daysOverdue;

    @Column(name = "fecha_ultimo_pago")
    private LocalDate lastPaymentDate;

    @Column(name = "monto_ultimo_pago", precision = 10, scale = 2)
    private BigDecimal lastPaymentAmount;

    public DebtInfo(BigDecimal capitalBalance, BigDecimal overdueInterest,
                   BigDecimal accumulatedLateFees, BigDecimal collectionFees,
                   BigDecimal currentDebt, Integer daysOverdue) {
        this.capitalBalance = capitalBalance;
        this.overdueInterest = overdueInterest;
        this.accumulatedLateFees = accumulatedLateFees;
        this.collectionFees = collectionFees;
        this.currentDebt = currentDebt;
        this.daysOverdue = daysOverdue;
        this.totalBalance = calculateTotalBalance();
    }

    private BigDecimal calculateTotalBalance() {
        BigDecimal total = BigDecimal.ZERO;
        if (capitalBalance != null) total = total.add(capitalBalance);
        if (overdueInterest != null) total = total.add(overdueInterest);
        if (accumulatedLateFees != null) total = total.add(accumulatedLateFees);
        if (collectionFees != null) total = total.add(collectionFees);
        return total;
    }

    public void updateLastPayment(LocalDate paymentDate, BigDecimal amount) {
        this.lastPaymentDate = paymentDate;
        this.lastPaymentAmount = amount;
    }
}
