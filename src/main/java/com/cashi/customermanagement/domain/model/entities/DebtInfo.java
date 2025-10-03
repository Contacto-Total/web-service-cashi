package com.cashi.customermanagement.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "customer_debt_info")
@Getter
@NoArgsConstructor
public class DebtInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "capital_balance", precision = 10, scale = 2)
    private BigDecimal capitalBalance;

    @Column(name = "overdue_interest", precision = 10, scale = 2)
    private BigDecimal overdueInterest;

    @Column(name = "accumulated_late_fees", precision = 10, scale = 2)
    private BigDecimal accumulatedLateFees;

    @Column(name = "collection_fees", precision = 10, scale = 2)
    private BigDecimal collectionFees;

    @Column(name = "total_balance", precision = 10, scale = 2)
    private BigDecimal totalBalance;

    @Column(name = "days_overdue")
    private Integer daysOverdue;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "last_payment_amount", precision = 10, scale = 2)
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
