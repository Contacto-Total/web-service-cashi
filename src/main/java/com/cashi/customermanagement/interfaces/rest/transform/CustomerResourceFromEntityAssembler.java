package com.cashi.customermanagement.interfaces.rest.transform;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import com.cashi.customermanagement.interfaces.rest.resources.*;

public class CustomerResourceFromEntityAssembler {

    public static CustomerResource toResourceFromEntity(Customer entity) {
        return new CustomerResource(
                entity.getId(),
                entity.getCustomerId(),
                entity.getFullName(),
                entity.getDocumentNumber() != null ? entity.getDocumentNumber().getType() : null,
                entity.getDocumentNumber() != null ? entity.getDocumentNumber().getNumber() : null,
                entity.getBirthDate(),
                entity.getAge()
        );
    }
}
