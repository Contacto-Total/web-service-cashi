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

    @Column(name = "saldo_total", precision = 10, scale = 2)
    private BigDecimal totalBalance;

    @Column(name = "dias_mora")
    private Integer daysOverdue;

    @Column(name = "fecha_ultimo_pago")
    private LocalDate lastPaymentDate;

    @Column(name = "monto_ultimo_pago", precision = 10, scale = 2)
    private BigDecimal lastPaymentAmount;

    public DebtInfo(BigDecimal capitalBalance, BigDecimal overdueInterest,
                   BigDecimal accumulatedLateFees, BigDecimal collectionFees,
                   Integer daysOverdue) {
        this.capitalBalance = capitalBalance;
        this.overdueInterest = overdueInterest;
        this.accumulatedLateFees = accumulatedLateFees;
        this.collectionFees = collectionFees;
        this.daysOverdue = daysOverdue;
        this.totalBalance = calculateTotalBalance();
    }

    private BigDecimal calculateTotalBalance() {
        return capitalBalance
                .add(overdueInterest)
                .add(accumulatedLateFees)
                .add(collectionFees);
    }

    public void updateLastPayment(LocalDate paymentDate, BigDecimal amount) {
        this.lastPaymentDate = paymentDate;
        this.lastPaymentAmount = amount;
    }
}
