package com.cashi.collectionmanagement.application.internal.queryservices;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.collectionmanagement.domain.model.queries.*;
import com.cashi.collectionmanagement.domain.services.ManagementQueryService;
import com.cashi.collectionmanagement.infrastructure.persistence.jpa.repositories.ManagementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ManagementQueryServiceImpl implements ManagementQueryService {

    private final ManagementRepository repository;

    public ManagementQueryServiceImpl(ManagementRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Management> handle(GetManagementByIdQuery query) {
        return repository.findById(query.id());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Management> handle(GetManagementsByCustomerQuery query) {
        return repository.findByCustomerId(query.customerId());
    }

    @Override
    public List<Management> handle(GetManagementsByAdvisorQuery query) {
        return repository.findByAdvisorId(query.advisorId());
    }

    @Override
    public List<Management> handle(GetManagementsByCampaignQuery query) {
        return repository.findByCampaign_Id(query.campaignId());
    }

    @Override
    public List<Management> handle(GetManagementsByDateRangeQuery query) {
        return repository.findByManagementDateBetween(query.startDate(), query.endDate());
    }
}
