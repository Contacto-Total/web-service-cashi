package com.cashi.paymentprocessing.interfaces.rest.transform;

import com.cashi.paymentprocessing.domain.model.aggregates.PaymentSchedule;
import com.cashi.paymentprocessing.domain.model.entities.Installment;
import com.cashi.paymentprocessing.interfaces.rest.resources.InstallmentResource;
import com.cashi.paymentprocessing.interfaces.rest.resources.PaymentScheduleResource;

import java.util.List;

public class PaymentScheduleResourceFromEntityAssembler {

    public static PaymentScheduleResource toResourceFromEntity(PaymentSchedule entity) {
        return new PaymentScheduleResource(
                entity.getId(),
                entity.getScheduleId() != null ? entity.getScheduleId().getScheduleId() : null,
                entity.getCustomerId(),
                entity.getManagementId(),
                entity.getTotalAmount(),
                entity.getNumberOfInstallments(),
                entity.getStartDate(),
                entity.getIsActive(),
                entity.getPaidAmount(),
                entity.getPendingAmount(),
                entity.getPaidInstallments(),
                entity.getPendingInstallments(),
                toInstallmentResources(entity.getInstallments()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static List<InstallmentResource> toInstallmentResources(List<Installment> installments) {
        if (installments == null) return List.of();
        return installments.stream()
                .map(inst -> new InstallmentResource(
                        inst.getId(),
                        inst.getInstallmentNumber(),
                        inst.getAmount(),
                        inst.getDueDate(),
                        inst.getPaidDate(),
                        inst.getStatus() != null ? inst.getStatus().getStatus() : null,
                        inst.getStatus() != null ? inst.getStatus().getDescription() : null
                ))
                .toList();
    }
}
