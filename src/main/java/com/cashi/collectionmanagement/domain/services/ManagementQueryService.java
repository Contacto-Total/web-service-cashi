package com.cashi.collectionmanagement.domain.services;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.collectionmanagement.domain.model.queries.*;

import java.util.List;
import java.util.Optional;

public interface ManagementQueryService {

    Optional<Management> handle(GetManagementByIdQuery query);

    List<Management> handle(GetManagementsByCustomerQuery query);

    List<Management> handle(GetManagementsByAdvisorQuery query);
}
