package com.cashi.paymentprocessing.domain.model.aggregates;

import com.cashi.paymentprocessing.domain.model.entities.Installment;
import com.cashi.paymentprocessing.domain.model.valueobjects.PaymentScheduleId;
import com.cashi.shared.domain.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cronogramas_pago")
@Getter
@NoArgsConstructor
public class PaymentSchedule extends AggregateRoot {

    @Embedded
    private PaymentScheduleId scheduleId;

    @Column(name = "id_cliente")
    private String customerId;

    @Column(name = "id_gestion")
    private String managementId;

    @Column(name = "monto_total")
    private BigDecimal totalAmount;

    @Column(name = "numero_cuotas")
    private Integer numberOfInstallments;

    @Column(name = "fecha_inicio")
    private LocalDate startDate;

    @Column(name = "esta_activo")
    private Boolean isActive;

    @Column(name = "tipo_cronograma")
    private String scheduleType; // "FINANCIERA" o "CONFIANZA"

    @Column(name = "monto_negociado")
    private BigDecimal negotiatedAmount; // Para tipo FINANCIERA

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "id_cronograma_pago")
    private List<Installment> installments = new ArrayList<>();

    // Constructor para cronogramas con cuotas manuales (NUEVO)
    public PaymentSchedule(String customerId, String managementId, String scheduleType, BigDecimal negotiatedAmount, List<InstallmentData> installmentDataList) {
        this.scheduleId = PaymentScheduleId.generate();
        this.customerId = customerId;
        this.managementId = managementId;
        this.scheduleType = scheduleType;
        this.negotiatedAmount = negotiatedAmount;
        this.numberOfInstallments = installmentDataList.size();
        this.isActive = true;

        // Calcular total amount sumando las cuotas
        this.totalAmount = installmentDataList.stream()
                .map(InstallmentData::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Fecha de inicio = fecha de la primera cuota
        this.startDate = installmentDataList.isEmpty() ? LocalDate.now() : installmentDataList.get(0).dueDate();

        // Crear cuotas desde los datos proporcionados
        for (InstallmentData data : installmentDataList) {
            installments.add(new Installment(data.installmentNumber(), data.amount(), data.dueDate()));
        }
    }

    // Record para pasar datos de cuotas al constructor
    public record InstallmentData(Integer installmentNumber, BigDecimal amount, LocalDate dueDate) {}

    // Constructor legacy para backward compatibility (si lo necesitas)
    @Deprecated
    public PaymentSchedule(String customerId, String managementId, BigDecimal totalAmount, Integer numberOfInstallments, LocalDate startDate) {
        this.scheduleId = PaymentScheduleId.generate();
        this.customerId = customerId;
        this.managementId = managementId;
        this.totalAmount = totalAmount;
        this.numberOfInstallments = numberOfInstallments;
        this.startDate = startDate;
        this.isActive = true;
        this.scheduleType = "LEGACY";
        generateInstallments();
    }

    private void generateInstallments() {
        BigDecimal installmentAmount = totalAmount.divide(BigDecimal.valueOf(numberOfInstallments), 2, BigDecimal.ROUND_HALF_UP);

        for (int i = 1; i <= numberOfInstallments; i++) {
            LocalDate dueDate = startDate.plusMonths(i - 1);
            installments.add(new Installment(i, installmentAmount, dueDate));
        }
    }

    public void markInstallmentAsPaid(Integer installmentNumber, LocalDate paidDate) {
        installments.stream()
                .filter(inst -> inst.getInstallmentNumber().equals(installmentNumber))
                .findFirst()
                .ifPresent(inst -> inst.markAsPaid(paidDate));
    }

    public void cancel() {
        this.isActive = false;
        installments.forEach(Installment::cancel);
    }

    public BigDecimal getPaidAmount() {
        return installments.stream()
                .filter(inst -> inst.getStatus().isCompleted())
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPendingAmount() {
        return totalAmount.subtract(getPaidAmount());
    }

    public Integer getPaidInstallments() {
        return (int) installments.stream()
                .filter(inst -> inst.getStatus().isCompleted())
                .count();
    }

    public Integer getPendingInstallments() {
        return numberOfInstallments - getPaidInstallments();
    }

    public boolean isFullyPaid() {
        return getPaidInstallments().equals(numberOfInstallments);
    }
}
