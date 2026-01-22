package com.cashi.paymentprocessing.interfaces.rest.controllers;

import com.cashi.paymentprocessing.domain.model.aggregates.PaymentSchedule;
import com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories.PaymentScheduleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Payment Schedules", description = "API para gestión de cronogramas de pago")
@RestController
@RequestMapping("/api/v1/payment-schedules")
@CrossOrigin(origins = "*")
public class PaymentScheduleController {

    private final PaymentScheduleRepository repository;

    public PaymentScheduleController(PaymentScheduleRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "Obtener cronogramas activos por cliente",
               description = "Retorna los cronogramas activos del cliente (típicamente uno)")
    @GetMapping("/customer/{customerId}/active")
    public ResponseEntity<List<PaymentSchedule>> getActiveSchedulesByCustomer(@PathVariable String customerId) {
        System.out.println("📊 Buscando cronogramas activos para cliente: " + customerId);

        List<PaymentSchedule> schedules = repository.findByCustomerIdAndIsActiveTrue(customerId);

        System.out.println("✅ Cronogramas activos encontrados: " + schedules.size());
        schedules.forEach(s -> {
            System.out.println("   - Cronograma ID: " + s.getScheduleId().getScheduleId());
            System.out.println("     • Número de cuotas: " + s.getNumberOfInstallments());
            System.out.println("     • Monto total: S/ " + s.getTotalAmount());
            System.out.println("     • Cuotas pendientes: " + s.getPendingInstallments());
        });

        return ResponseEntity.ok(schedules);
    }

    @Operation(summary = "Obtener cronograma por ID de gestión")
    @GetMapping("/management/{managementId}")
    public ResponseEntity<PaymentSchedule> getByManagementId(@PathVariable String managementId) {
        System.out.println("📊 Buscando cronograma para gestión: " + managementId);

        List<PaymentSchedule> schedules = repository.findByManagementId(managementId);

        if (!schedules.isEmpty()) {
            PaymentSchedule schedule = schedules.get(0);
            System.out.println("✅ Cronograma encontrado - ID: " + schedule.getScheduleId().getScheduleId());
            System.out.println("   - Número de cuotas: " + schedule.getNumberOfInstallments());
            System.out.println("   - Monto total: S/ " + schedule.getTotalAmount());
            return ResponseEntity.ok(schedule);
        } else {
            System.out.println("ℹ️  No se encontró cronograma para esta gestión");
            return ResponseEntity.notFound().build();
        }
    }
}
