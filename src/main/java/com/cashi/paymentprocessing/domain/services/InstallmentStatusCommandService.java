package com.cashi.paymentprocessing.domain.services;

import com.cashi.paymentprocessing.domain.model.entities.InstallmentStatusHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface InstallmentStatusCommandService {

    /**
     * Registra el estado inicial de una cuota (PENDIENTE)
     */
    InstallmentStatusHistory registerInitialStatus(Long installmentId, String managementId, String registeredBy);

    /**
     * Registra un pago de cuota (COMPLETADO)
     */
    InstallmentStatusHistory registerPayment(
            Long installmentId,
            String managementId,
            LocalDateTime paymentDate,
            BigDecimal amountPaid,
            String observations,
            String registeredBy
    );

    /**
     * Marca una cuota como vencida (VENCIDO)
     */
    InstallmentStatusHistory registerOverdue(
            Long installmentId,
            String managementId,
            String observations,
            String registeredBy
    );

    /**
     * Cancela una cuota (CANCELADO)
     */
    InstallmentStatusHistory registerCancellation(
            Long installmentId,
            String managementId,
            String observations,
            String registeredBy
    );
}
