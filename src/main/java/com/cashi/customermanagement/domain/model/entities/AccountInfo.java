package com.cashi.customermanagement.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "customer_account_info")
@Getter
@NoArgsConstructor
public class AccountInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "original_amount", precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "term_months")
    private Integer termMonths;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    public AccountInfo(String accountNumber, String productType, LocalDate disbursementDate,
                      BigDecimal originalAmount, Integer termMonths, BigDecimal interestRate) {
        this.accountNumber = accountNumber;
        this.productType = productType;
        this.disbursementDate = disbursementDate;
        this.originalAmount = originalAmount;
        this.termMonths = termMonths;
        this.interestRate = interestRate;
    }
}
