package com.cashi.collectionmanagement.interfaces.rest.transform;

import com.cashi.collectionmanagement.domain.model.commands.RegisterPaymentCommand;
import com.cashi.collectionmanagement.interfaces.rest.resources.RegisterPaymentRequest;

public class RegisterPaymentCommandFromResourceAssembler {

    public static RegisterPaymentCommand toCommandFromResource(Long managementId, RegisterPaymentRequest resource) {
        return new RegisterPaymentCommand(
                managementId,
                resource.amount(),
                resource.scheduledDate(),
                resource.paymentMethodType(),
                resource.paymentMethodDetails(),
                resource.voucherNumber(),
                resource.bankName()
        );
    }
}
