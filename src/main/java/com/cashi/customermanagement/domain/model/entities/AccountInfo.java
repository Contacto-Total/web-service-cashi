package com.cashi.customermanagement.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "informacion_cuenta")
@Getter
@NoArgsConstructor
public class AccountInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cuenta", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "tipo_producto", nullable = false)
    private String productType;

    @Column(name = "fecha_desembolso")
    private LocalDate disbursementDate;

    @Column(name = "monto_original", precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "plazo_meses")
    private Integer termMonths;

    @Column(name = "tasa_interes", precision = 5, scale = 2)
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
