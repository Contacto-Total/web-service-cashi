package com.cashi.paymentprocessing.application.internal.commandservices;

import com.cashi.paymentprocessing.domain.model.entities.InstallmentStatusHistory;
import com.cashi.paymentprocessing.domain.services.InstallmentStatusCommandService;
import com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories.InstallmentStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class InstallmentStatusCommandServiceImpl implements InstallmentStatusCommandService {

    private final InstallmentStatusHistoryRepository statusHistoryRepository;

    public InstallmentStatusCommandServiceImpl(InstallmentStatusHistoryRepository statusHistoryRepository) {
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Override
    @Transactional
    public InstallmentStatusHistory registerInitialStatus(Long installmentId, String managementId, String registeredBy) {
        System.out.println("📝 Registrando estado inicial para cuota ID: " + installmentId);

        InstallmentStatusHistory history = InstallmentStatusHistory.createInitialStatus(
                installmentId,
                managementId,
                registeredBy
        );

        InstallmentStatusHistory saved = statusHistoryRepository.save(history);
        System.out.println("✅ Estado inicial registrado - Historial ID: " + saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public InstallmentStatusHistory registerPayment(
            Long installmentId,
            String managementId,
            LocalDateTime paymentDate,
            BigDecimal amountPaid,
            String observations,
            String registeredBy) {

        System.out.println("💰 Registrando pago para cuota ID: " + installmentId);
        System.out.println("   - Fecha de pago: " + paymentDate);
        System.out.println("   - Monto: S/ " + amountPaid);

        InstallmentStatusHistory history = InstallmentStatusHistory.createPaymentStatus(
                installmentId,
                managementId,
                paymentDate,
                amountPaid,
                observations,
                registeredBy
        );

        InstallmentStatusHistory saved = statusHistoryRepository.save(history);
        System.out.println("✅ Pago registrado - Historial ID: " + saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public InstallmentStatusHistory registerOverdue(
            Long installmentId,
            String managementId,
            String observations,
            String registeredBy) {

        System.out.println("⏰ Registrando vencimiento para cuota ID: " + installmentId);

        InstallmentStatusHistory history = InstallmentStatusHistory.createOverdueStatus(
                installmentId,
                managementId,
                observations,
                registeredBy
        );

        InstallmentStatusHistory saved = statusHistoryRepository.save(history);
        System.out.println("✅ Vencimiento registrado - Historial ID: " + saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public InstallmentStatusHistory registerCancellation(
            Long installmentId,
            String managementId,
            String observations,
            String registeredBy) {

        System.out.println("❌ Registrando cancelación para cuota ID: " + installmentId);

        InstallmentStatusHistory history = InstallmentStatusHistory.createCancellationStatus(
                installmentId,
                managementId,
                observations,
                registeredBy
        );

        InstallmentStatusHistory saved = statusHistoryRepository.save(history);
        System.out.println("✅ Cancelación registrada - Historial ID: " + saved.getId());

        return saved;
    }
}
