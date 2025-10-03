package com.cashi.paymentprocessing.interfaces.rest.transform;

import com.cashi.paymentprocessing.domain.model.commands.CreatePaymentScheduleCommand;
import com.cashi.paymentprocessing.interfaces.rest.resources.CreatePaymentScheduleRequest;

public class CreatePaymentScheduleCommandFromResourceAssembler {

    public static CreatePaymentScheduleCommand toCommandFromResource(CreatePaymentScheduleRequest resource) {
        return new CreatePaymentScheduleCommand(
                resource.customerId(),
                resource.managementId(),
                resource.totalAmount(),
                resource.numberOfInstallments(),
                resource.startDate()
        );
    }
}
