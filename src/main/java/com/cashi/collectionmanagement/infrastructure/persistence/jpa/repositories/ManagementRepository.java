package com.cashi.collectionmanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ManagementRepository extends JpaRepository<Management, Long> {

    // NOTE: findById(Long id) is inherited from JpaRepository
    // No need for findByManagementId_ManagementId since ManagementId is removed

    List<Management> findByCustomerId(String customerId);

    List<Management> findByAdvisorId(String advisorId);

    List<Management> findByCampaign_Id(Long campaignId);

    List<Management> findByManagementDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Management> findByCustomerIdOrderByManagementDateDesc(String customerId);

    List<Management> findByAdvisorIdAndManagementDateBetween(String advisorId, LocalDateTime startDate, LocalDateTime endDate);
}
