package com.cashi.collectionmanagement.domain.services;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.collectionmanagement.domain.model.commands.*;

public interface ManagementCommandService {

    Management handle(CreateManagementCommand command);

    Management handle(UpdateManagementCommand command);

    Management handle(StartCallCommand command);

    Management handle(EndCallCommand command);

    Management handle(RegisterPaymentCommand command);
}
