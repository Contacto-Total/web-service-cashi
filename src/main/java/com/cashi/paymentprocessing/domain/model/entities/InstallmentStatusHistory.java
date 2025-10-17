package com.cashi.paymentprocessing.domain.model.entities;

import com.cashi.paymentprocessing.domain.model.valueobjects.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cuotas_historial_estados")
@Getter
@NoArgsConstructor
public class InstallmentStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_cuota", nullable = false)
    private Long installmentId;

    @Column(name = "id_gestion")
    private String managementId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "status", column = @Column(name = "estado")),
        @AttributeOverride(name = "description", column = @Column(name = "descripcion_estado"))
    })
    private PaymentStatus status;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime changeDate;

    @Column(name = "fecha_pago_real")
    private LocalDateTime actualPaymentDate;

    @Column(name = "monto_pagado")
    private java.math.BigDecimal amountPaid;

    @Column(name = "observaciones", length = 500)
    private String observations;

    @Column(name = "usuario_registro", length = 50)
    private String registeredBy;

    public InstallmentStatusHistory(Long installmentId, String managementId, PaymentStatus status,
                                   LocalDateTime actualPaymentDate, java.math.BigDecimal amountPaid,
                                   String observations, String registeredBy) {
        this.installmentId = installmentId;
        this.managementId = managementId;
        this.status = status;
        this.changeDate = LocalDateTime.now();
        this.actualPaymentDate = actualPaymentDate;
        this.amountPaid = amountPaid;
        this.observations = observations;
        this.registeredBy = registeredBy;
    }

    // Método estático para crear registro inicial (PENDIENTE)
    public static InstallmentStatusHistory createInitialStatus(Long installmentId, String managementId, String registeredBy) {
        return new InstallmentStatusHistory(
            installmentId,
            managementId,
            PaymentStatus.pending(),
            null,
            null,
            "Estado inicial - Promesa de pago",
            registeredBy
        );
    }

    // Método estático para crear registro de pago
    public static InstallmentStatusHistory createPaymentStatus(Long installmentId, String managementId,
                                                               LocalDateTime paymentDate, java.math.BigDecimal amount,
                                                               String observations, String registeredBy) {
        return new InstallmentStatusHistory(
            installmentId,
            managementId,
            PaymentStatus.completed(),
            paymentDate,
            amount,
            observations,
            registeredBy
        );
    }

    // Método estático para marcar como vencido
    public static InstallmentStatusHistory createOverdueStatus(Long installmentId, String managementId,
                                                               String observations, String registeredBy) {
        return new InstallmentStatusHistory(
            installmentId,
            managementId,
            PaymentStatus.overdue(),
            null,
            null,
            observations,
            registeredBy
        );
    }

    // Método estático para cancelar cuota
    public static InstallmentStatusHistory createCancellationStatus(Long installmentId, String managementId,
                                                                   String observations, String registeredBy) {
        return new InstallmentStatusHistory(
            installmentId,
            managementId,
            PaymentStatus.cancelled(),
            null,
            null,
            observations,
            registeredBy
        );
    }
}
