package com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories;

import com.cashi.paymentprocessing.domain.model.aggregates.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

    Optional<PaymentSchedule> findByScheduleId_ScheduleId(String scheduleId);

    List<PaymentSchedule> findByCustomerId(String customerId);

    List<PaymentSchedule> findByCustomerIdAndIsActiveTrue(String customerId);

    List<PaymentSchedule> findByManagementId(String managementId);
}
