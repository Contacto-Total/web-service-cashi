package com.cashi.paymentprocessing.interfaces.rest.transform;

import com.cashi.paymentprocessing.domain.model.commands.CreatePaymentScheduleCommand;
import com.cashi.paymentprocessing.interfaces.rest.resources.CreatePaymentScheduleRequest;

import java.util.stream.Collectors;

public class CreatePaymentScheduleCommandFromResourceAssembler {

    public static CreatePaymentScheduleCommand toCommandFromResource(CreatePaymentScheduleRequest resource) {
        var installments = resource.installments().stream()
                .map(inst -> new CreatePaymentScheduleCommand.InstallmentData(
                        inst.installmentNumber(),
                        inst.amount(),
                        inst.dueDate()
                ))
                .collect(Collectors.toList());

        return new CreatePaymentScheduleCommand(
                resource.customerId(),
                resource.managementId(),
                resource.scheduleType(),
                resource.negotiatedAmount(),
                installments
        );
    }
}
