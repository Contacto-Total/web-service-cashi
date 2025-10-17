package com.cashi.paymentprocessing.interfaces.rest.controllers;

import com.cashi.paymentprocessing.domain.model.aggregates.PaymentSchedule;
import com.cashi.paymentprocessing.domain.model.entities.InstallmentStatusHistory;
import com.cashi.paymentprocessing.domain.services.InstallmentStatusCommandService;
import com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories.InstallmentStatusHistoryRepository;
import com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories.PaymentScheduleRepository;
import com.cashi.paymentprocessing.interfaces.rest.resources.InstallmentStatusHistoryResource;
import com.cashi.paymentprocessing.interfaces.rest.resources.UpdateInstallmentStatusResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Payment Schedules", description = "API para gestión de cronogramas de pago")
@RestController
@RequestMapping("/api/v1/payment-schedules")
@CrossOrigin(origins = "*")
public class PaymentScheduleController {

    private final PaymentScheduleRepository repository;
    private final InstallmentStatusCommandService statusService;
    private final InstallmentStatusHistoryRepository statusHistoryRepository;

    public PaymentScheduleController(PaymentScheduleRepository repository,
                                    InstallmentStatusCommandService statusService,
                                    InstallmentStatusHistoryRepository statusHistoryRepository) {
        this.repository = repository;
        this.statusService = statusService;
        this.statusHistoryRepository = statusHistoryRepository;
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
            // Retornar el primero (normalmente debería haber solo uno por gestión)
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

    @Operation(summary = "Actualizar estado de una cuota")
    @PostMapping("/installments/{installmentId}/status")
    public ResponseEntity<InstallmentStatusHistoryResource> updateInstallmentStatus(
            @PathVariable Long installmentId,
            @RequestBody UpdateInstallmentStatusResource request) {

        System.out.println("🔄 Actualizando estado de cuota ID: " + installmentId);
        System.out.println("   - Nuevo estado: " + request.status());

        try {
            InstallmentStatusHistory history;

            switch (request.status().toUpperCase()) {
                case "COMPLETADO", "COMPLETED" -> {
                    if (request.paymentDate() == null || request.amountPaid() == null) {
                        return ResponseEntity.badRequest().build();
                    }
                    history = statusService.registerPayment(
                            installmentId,
                            null, // managementId se puede obtener de la cuota si es necesario
                            request.paymentDate(),
                            request.amountPaid(),
                            request.observations(),
                            request.registeredBy()
                    );
                }
                case "VENCIDO", "OVERDUE" -> history = statusService.registerOverdue(
                        installmentId,
                        null,
                        request.observations(),
                        request.registeredBy()
                );
                case "CANCELADO", "CANCELLED" -> history = statusService.registerCancellation(
                        installmentId,
                        null,
                        request.observations(),
                        request.registeredBy()
                );
                default -> {
                    System.out.println("❌ Estado no válido: " + request.status());
                    return ResponseEntity.badRequest().build();
                }
            }

            System.out.println("✅ Estado actualizado exitosamente");

            InstallmentStatusHistoryResource resource = new InstallmentStatusHistoryResource(
                    history.getId(),
                    history.getInstallmentId(),
                    history.getManagementId(),
                    history.getStatus().getStatus(),
                    history.getStatus().getDescription(),
                    history.getChangeDate(),
                    history.getActualPaymentDate(),
                    history.getAmountPaid(),
                    history.getObservations(),
                    history.getRegisteredBy()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(resource);

        } catch (Exception e) {
            System.err.println("❌ Error actualizando estado: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Obtener historial completo de una cuota")
    @GetMapping("/installments/{installmentId}/history")
    public ResponseEntity<List<InstallmentStatusHistoryResource>> getInstallmentHistory(
            @PathVariable Long installmentId) {

        System.out.println("📜 Obteniendo historial de cuota ID: " + installmentId);

        List<InstallmentStatusHistory> history = statusHistoryRepository
                .findByInstallmentIdOrderByChangeDateDesc(installmentId);

        List<InstallmentStatusHistoryResource> resources = history.stream()
                .map(h -> new InstallmentStatusHistoryResource(
                        h.getId(),
                        h.getInstallmentId(),
                        h.getManagementId(),
                        h.getStatus().getStatus(),
                        h.getStatus().getDescription(),
                        h.getChangeDate(),
                        h.getActualPaymentDate(),
                        h.getAmountPaid(),
                        h.getObservations(),
                        h.getRegisteredBy()
                ))
                .collect(Collectors.toList());

        System.out.println("✅ Historial encontrado: " + resources.size() + " registros");

        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Obtener último estado de cada cuota de una gestión")
    @GetMapping("/management/{managementId}/latest-status")
    public ResponseEntity<List<InstallmentStatusHistoryResource>> getLatestStatusByManagement(
            @PathVariable String managementId) {

        System.out.println("📊 Obteniendo últimos estados para gestión: " + managementId);

        List<InstallmentStatusHistory> latestStatuses = statusHistoryRepository
                .findLatestStatusByManagementId(managementId);

        List<InstallmentStatusHistoryResource> resources = latestStatuses.stream()
                .map(h -> new InstallmentStatusHistoryResource(
                        h.getId(),
                        h.getInstallmentId(),
                        h.getManagementId(),
                        h.getStatus().getStatus(),
                        h.getStatus().getDescription(),
                        h.getChangeDate(),
                        h.getActualPaymentDate(),
                        h.getAmountPaid(),
                        h.getObservations(),
                        h.getRegisteredBy()
                ))
                .collect(Collectors.toList());

        System.out.println("✅ Estados encontrados: " + resources.size() + " cuotas");

        return ResponseEntity.ok(resources);
    }
}
