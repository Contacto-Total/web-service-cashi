package com.cashi.customermanagement.interfaces.rest.controllers;

import com.cashi.customermanagement.application.internal.queryservices.CustomerQueryServiceImpl;
import com.cashi.customermanagement.interfaces.rest.resources.CustomerResource;
import com.cashi.customermanagement.interfaces.rest.transform.CustomerResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "Gestión de clientes, cuentas y deudas")
public class CustomerController {

    private final CustomerQueryServiceImpl queryService;

    public CustomerController(CustomerQueryServiceImpl queryService) {
        this.queryService = queryService;
    }

    @Operation(summary = "Obtener todos los clientes", description = "Retorna lista completa de clientes con su información de cuenta y deuda")
    @ApiResponse(responseCode = "200", description = "Lista de clientes obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<CustomerResource>> getAllCustomers() {
        var customers = queryService.getAllCustomers();
        var resources = customers.stream()
                .map(CustomerResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Obtener cliente por ID", description = "Retorna un cliente específico con toda su información")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResource> getCustomerById(
            @Parameter(description = "ID del cliente", example = "CUST-001") @PathVariable String customerId) {
        return queryService.getCustomerById(customerId)
                .map(customer -> ResponseEntity.ok(
                        CustomerResourceFromEntityAssembler.toResourceFromEntity(customer)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Buscar cliente por documento", description = "Busca un cliente por su número de documento")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/document/{documentNumber}")
    public ResponseEntity<CustomerResource> getCustomerByDocument(
            @Parameter(description = "Número de documento", example = "45678912") @PathVariable String documentNumber) {
        return queryService.getCustomerByDocumentNumber(documentNumber)
                .map(customer -> ResponseEntity.ok(
                        CustomerResourceFromEntityAssembler.toResourceFromEntity(customer)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Buscar clientes", description = "Busca clientes por nombre, documento o cuenta")
    @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos")
    @GetMapping("/search")
    public ResponseEntity<List<CustomerResource>> searchCustomers(
            @Parameter(description = "Término de búsqueda") @RequestParam String query) {
        var customers = queryService.searchCustomers(query);
        var resources = customers.stream()
                .map(CustomerResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
