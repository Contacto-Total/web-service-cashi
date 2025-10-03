package com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories;

import com.cashi.paymentprocessing.domain.model.aggregates.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId_PaymentId(String paymentId);

    List<Payment> findByCustomerId(String customerId);

    List<Payment> findByCustomerIdAndStatus_Status(String customerId, String status);

    List<Payment> findByManagementId(String managementId);

    List<Payment> findByCustomerIdOrderByPaymentDateDesc(String customerId);
}
