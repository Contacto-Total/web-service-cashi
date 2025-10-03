package com.cashi.paymentprocessing.interfaces.rest.transform;

import com.cashi.paymentprocessing.domain.model.commands.CreatePaymentCommand;
import com.cashi.paymentprocessing.interfaces.rest.resources.CreatePaymentRequest;

public class CreatePaymentCommandFromResourceAssembler {

    public static CreatePaymentCommand toCommandFromResource(CreatePaymentRequest resource) {
        return new CreatePaymentCommand(
                resource.customerId(),
                resource.managementId(),
                resource.amount(),
                resource.paymentDate(),
                resource.paymentMethod(),
                resource.voucherNumber(),
                resource.bankName(),
                resource.notes()
        );
    }
}
