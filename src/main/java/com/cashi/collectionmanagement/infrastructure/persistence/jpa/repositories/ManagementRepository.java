package com.cashi.collectionmanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagementRepository extends JpaRepository<Management, Long> {

    List<Management> findByCustomerId(String customerId);

    List<Management> findByAdvisorId(String advisorId);
}
