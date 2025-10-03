package com.cashi.customermanagement.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtInfoResource(
        BigDecimal capitalBalance,
        BigDecimal overdueInterest,
        BigDecimal accumulatedLateFees,
        BigDecimal collectionFees,
        BigDecimal totalBalance,
        Integer daysOverdue,
        LocalDate lastPaymentDate,
        BigDecimal lastPaymentAmount
) {
}
