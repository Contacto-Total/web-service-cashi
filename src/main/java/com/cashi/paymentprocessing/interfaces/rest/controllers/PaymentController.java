package com.cashi.paymentprocessing.interfaces.rest.controllers;

import com.cashi.paymentprocessing.application.internal.commandservices.PaymentCommandServiceImpl;
import com.cashi.paymentprocessing.application.internal.queryservices.PaymentQueryServiceImpl;
import com.cashi.paymentprocessing.domain.model.commands.CancelPaymentCommand;
import com.cashi.paymentprocessing.domain.model.commands.CancelPaymentScheduleCommand;
import com.cashi.paymentprocessing.domain.model.commands.ConfirmPaymentCommand;
import com.cashi.paymentprocessing.domain.model.commands.RecordInstallmentPaymentCommand;
import com.cashi.paymentprocessing.domain.model.queries.*;
import com.cashi.paymentprocessing.interfaces.rest.resources.*;
import com.cashi.paymentprocessing.interfaces.rest.transform.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Processing", description = "Procesamiento de pagos y cronogramas de cuotas")
public class PaymentController {

    private final PaymentCommandServiceImpl commandService;
    private final PaymentQueryServiceImpl queryService;

    public PaymentController(PaymentCommandServiceImpl commandService, PaymentQueryServiceImpl queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @Operation(summary = "Registrar nuevo pago", description = "Crea un registro de pago con estado pendiente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pago creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<PaymentResource> createPayment(@RequestBody CreatePaymentRequest request) {
        var command = CreatePaymentCommandFromResourceAssembler.toCommandFromResource(request);
        var payment = commandService.handle(command);
        var resource = PaymentResourceFromEntityAssembler.toResourceFromEntity(payment);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Operation(summary = "Obtener pago por ID", description = "Retorna información detallada de un pago")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pago encontrado"),
        @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResource> getPaymentById(
            @Parameter(description = "ID del pago") @PathVariable String paymentId) {
        var query = new GetPaymentByIdQuery(paymentId);
        return queryService.handle(query)
                .map(payment -> ResponseEntity.ok(
                        PaymentResourceFromEntityAssembler.toResourceFromEntity(payment)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Confirmar pago", description = "Confirma un pago registrado y actualiza su estado a completado")
    @ApiResponse(responseCode = "200", description = "Pago confirmado exitosamente")
    @PostMapping("/{paymentId}/confirm")
    public ResponseEntity<PaymentResource> confirmPayment(
            @Parameter(description = "ID del pago") @PathVariable String paymentId,
            @RequestBody ConfirmPaymentRequest request) {
        var command = new ConfirmPaymentCommand(paymentId, request.transactionId());
        var payment = commandService.handle(command);
        var resource = PaymentResourceFromEntityAssembler.toResourceFromEntity(payment);
        return ResponseEntity.ok(resource);
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResource> cancelPayment(@PathVariable String paymentId) {
        var command = new CancelPaymentCommand(paymentId);
        var payment = commandService.handle(command);
        var resource = PaymentResourceFromEntityAssembler.toResourceFromEntity(payment);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<PaymentResource>> getPaymentsByCustomer(@PathVariable String customerId) {
        var query = new GetPaymentsByCustomerQuery(customerId);
        var payments = queryService.handle(query);
        var resources = payments.stream()
                .map(PaymentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/customer/{customerId}/pending")
    public ResponseEntity<List<PaymentResource>> getPendingPayments(@PathVariable String customerId) {
        var query = new GetPendingPaymentsQuery(customerId);
        var payments = queryService.handle(query);
        var resources = payments.stream()
                .map(PaymentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @PostMapping("/schedules")
    public ResponseEntity<PaymentScheduleResource> createPaymentSchedule(@RequestBody CreatePaymentScheduleRequest request) {
        var command = CreatePaymentScheduleCommandFromResourceAssembler.toCommandFromResource(request);
        var schedule = commandService.handle(command);
        var resource = PaymentScheduleResourceFromEntityAssembler.toResourceFromEntity(schedule);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @GetMapping("/schedules/{scheduleId}")
    public ResponseEntity<PaymentScheduleResource> getPaymentScheduleById(@PathVariable String scheduleId) {
        var query = new GetPaymentScheduleByIdQuery(scheduleId);
        return queryService.handle(query)
                .map(schedule -> ResponseEntity.ok(
                        PaymentScheduleResourceFromEntityAssembler.toResourceFromEntity(schedule)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/schedules/customer/{customerId}")
    public ResponseEntity<List<PaymentScheduleResource>> getPaymentSchedulesByCustomer(@PathVariable String customerId) {
        var query = new GetPaymentSchedulesByCustomerQuery(customerId);
        var schedules = queryService.handle(query);
        var resources = schedules.stream()
                .map(PaymentScheduleResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @PostMapping("/schedules/{scheduleId}/installments")
    public ResponseEntity<PaymentScheduleResource> recordInstallmentPayment(
            @PathVariable String scheduleId,
            @RequestBody RecordInstallmentPaymentRequest request) {
        var command = new RecordInstallmentPaymentCommand(
                scheduleId,
                request.installmentNumber(),
                request.paidDate()
        );
        var schedule = commandService.handle(command);
        var resource = PaymentScheduleResourceFromEntityAssembler.toResourceFromEntity(schedule);
        return ResponseEntity.ok(resource);
    }

    @PostMapping("/schedules/{scheduleId}/cancel")
    public ResponseEntity<PaymentScheduleResource> cancelPaymentSchedule(@PathVariable String scheduleId) {
        var command = new CancelPaymentScheduleCommand(scheduleId);
        var schedule = commandService.handle(command);
        var resource = PaymentScheduleResourceFromEntityAssembler.toResourceFromEntity(schedule);
        return ResponseEntity.ok(resource);
    }
}
