package com.cashi.paymentprocessing.interfaces.rest.transform;

import com.cashi.paymentprocessing.domain.model.aggregates.Payment;
import com.cashi.paymentprocessing.interfaces.rest.resources.PaymentResource;

public class PaymentResourceFromEntityAssembler {

    public static PaymentResource toResourceFromEntity(Payment entity) {
        return new PaymentResource(
                entity.getId(),
                entity.getPaymentId() != null ? entity.getPaymentId().getPaymentId() : null,
                entity.getCustomerId(),
                entity.getManagementId(),
                entity.getAmount(),
                entity.getPaymentDate(),
                entity.getPaymentMethod(),
                entity.getStatus() != null ? entity.getStatus().getStatus() : null,
                entity.getStatus() != null ? entity.getStatus().getDescription() : null,
                entity.getTransactionId() != null ? entity.getTransactionId().getTransactionId() : null,
                entity.getVoucherNumber(),
                entity.getBankName(),
                entity.getConfirmedAt(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
