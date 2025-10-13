package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.collectionmanagement.interfaces.rest.resources.CallDetailResource;
import com.cashi.collectionmanagement.interfaces.rest.resources.ManagementResource;
import com.cashi.collectionmanagement.interfaces.rest.resources.PaymentDetailResource;

public class ManagementResourceFromEntityAssembler {

    public static ManagementResource toResourceFromEntity(Management entity) {
        return new ManagementResource(
                entity.getId(),
                entity.getManagementId() != null ? entity.getManagementId().getManagementId() : null,
                entity.getCustomerId(),
                entity.getAdvisorId(),
                entity.getCampaignId(),
                entity.getManagementDate(),
                // Clasificación y Tipificación
                entity.getClassificationCode(),
                entity.getClassificationDescription(),
                entity.getTypificationCode(),
                entity.getTypificationDescription(),
                entity.getTypificationRequiresPayment(),
                entity.getTypificationRequiresSchedule(),
                toCallDetailResource(entity),
                toPaymentDetailResource(entity),
                entity.getObservations(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static CallDetailResource toCallDetailResource(Management entity) {
        if (entity.getCallDetail() == null) return null;
        var call = entity.getCallDetail();
        return new CallDetailResource(
                call.getPhoneNumber(),
                call.getStartTime(),
                call.getEndTime(),
                call.getDurationSeconds()
        );
    }

    private static PaymentDetailResource toPaymentDetailResource(Management entity) {
        if (entity.getPaymentDetail() == null) return null;
        var payment = entity.getPaymentDetail();
        return new PaymentDetailResource(
                payment.getAmount(),
                payment.getScheduledDate(),
                payment.getPaymentMethod() != null ? payment.getPaymentMethod().getType() : null,
                payment.getPaymentMethod() != null ? payment.getPaymentMethod().getDetails() : null,
                payment.getVoucherNumber(),
                payment.getBankName()
        );
    }
}
