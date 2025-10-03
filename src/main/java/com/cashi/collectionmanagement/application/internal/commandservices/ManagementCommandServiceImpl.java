package com.cashi.collectionmanagement.application.internal.commandservices;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.collectionmanagement.domain.model.commands.*;
import com.cashi.collectionmanagement.domain.model.entities.CallDetail;
import com.cashi.collectionmanagement.domain.model.entities.PaymentDetail;
import com.cashi.collectionmanagement.domain.model.valueobjects.ContactResult;
import com.cashi.collectionmanagement.domain.model.valueobjects.ManagementType;
import com.cashi.collectionmanagement.domain.model.valueobjects.PaymentMethod;
import com.cashi.collectionmanagement.domain.services.ManagementCommandService;
import com.cashi.collectionmanagement.infrastructure.persistence.jpa.repositories.ManagementRepository;
import org.springframework.stereotype.Service;

@Service
public class ManagementCommandServiceImpl implements ManagementCommandService {

    private final ManagementRepository repository;

    public ManagementCommandServiceImpl(ManagementRepository repository) {
        this.repository = repository;
    }

    @Override
    public Management handle(CreateManagementCommand command) {
        var management = new Management(
            command.customerId(),
            command.advisorId(),
            command.campaignId()
        );

        if (command.contactResultCode() != null) {
            management.setContactResult(new ContactResult(
                command.contactResultCode(),
                command.contactResultDescription()
            ));
        }

        if (command.managementTypeCode() != null) {
            management.setManagementType(new ManagementType(
                command.managementTypeCode(),
                command.managementTypeDescription(),
                command.managementTypeRequiresPayment(),
                command.managementTypeRequiresSchedule()
            ));
        }

        if (command.observations() != null) {
            management.setObservations(command.observations());
        }

        return repository.save(management);
    }

    @Override
    public Management handle(UpdateManagementCommand command) {
        var management = repository.findByManagementId_ManagementId(command.managementId())
            .orElseThrow(() -> new IllegalArgumentException("Management not found: " + command.managementId()));

        var contactResult = new ContactResult(
            command.contactResultCode(),
            command.contactResultDescription()
        );

        var managementType = new ManagementType(
            command.managementTypeCode(),
            command.managementTypeDescription(),
            command.managementTypeRequiresPayment(),
            command.managementTypeRequiresSchedule()
        );

        management.updateManagement(contactResult, managementType, command.observations());

        return repository.save(management);
    }

    @Override
    public Management handle(StartCallCommand command) {
        var management = repository.findByManagementId_ManagementId(command.managementId())
            .orElseThrow(() -> new IllegalArgumentException("Management not found: " + command.managementId()));

        var callDetail = new CallDetail(command.phoneNumber(), command.startTime());
        management.setCallDetail(callDetail);

        return repository.save(management);
    }

    @Override
    public Management handle(EndCallCommand command) {
        var management = repository.findByManagementId_ManagementId(command.managementId())
            .orElseThrow(() -> new IllegalArgumentException("Management not found: " + command.managementId()));

        if (management.getCallDetail() != null) {
            management.getCallDetail().endCall(command.endTime());
        }

        return repository.save(management);
    }

    @Override
    public Management handle(RegisterPaymentCommand command) {
        var management = repository.findByManagementId_ManagementId(command.managementId())
            .orElseThrow(() -> new IllegalArgumentException("Management not found: " + command.managementId()));

        var paymentMethod = new PaymentMethod(
            command.paymentMethodType(),
            command.paymentMethodDetails()
        );

        var paymentDetail = new PaymentDetail(
            command.amount(),
            command.scheduledDate(),
            paymentMethod
        );

        if (command.voucherNumber() != null) {
            paymentDetail.setVoucherDetails(command.voucherNumber(), command.bankName());
        }

        management.setPaymentDetail(paymentDetail);

        return repository.save(management);
    }
}
