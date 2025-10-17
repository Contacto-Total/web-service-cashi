package com.cashi.paymentprocessing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateInstallmentStatusResource(
    String status, // "COMPLETADO", "VENCIDO", "CANCELADO"
    LocalDateTime paymentDate, // Solo para COMPLETADO
    BigDecimal amountPaid, // Solo para COMPLETADO
    String observations,
    String registeredBy
) {
}
