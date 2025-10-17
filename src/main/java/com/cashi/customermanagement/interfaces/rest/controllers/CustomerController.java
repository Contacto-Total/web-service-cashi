package com.cashi.customermanagement.interfaces.rest.controllers;

import com.cashi.customermanagement.application.internal.queryservices.CustomerQueryServiceImpl;
import com.cashi.customermanagement.domain.model.valueobjects.CustomerDataMapping;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerRepository;
import com.cashi.customermanagement.interfaces.rest.resources.CustomerResource;
import com.cashi.customermanagement.interfaces.rest.transform.CustomerResourceFromEntityAssembler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "Gestión de clientes, cuentas y deudas")
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerQueryServiceImpl queryService;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    public CustomerController(CustomerQueryServiceImpl queryService,
                            CustomerRepository customerRepository,
                            ObjectMapper objectMapper) {
        this.queryService = queryService;
        this.customerRepository = customerRepository;
        this.objectMapper = objectMapper;
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

    @Operation(summary = "Obtener cliente por código de documento y tenant",
               description = "Busca un cliente por su código de documento dentro de un tenant específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/by-document")
    public ResponseEntity<CustomerResource> getCustomerByDocumentCode(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Código de documento", example = "D000041692138") @RequestParam String documentCode) {

        System.out.println("🔍 Buscando cliente: tenantId=" + tenantId + ", documentCode=" + documentCode);

        return customerRepository.findByTenantIdAndDocumentCode(tenantId, documentCode)
                .map(customer -> {
                    System.out.println("✅ Cliente encontrado: " + customer.getFullName());
                    return ResponseEntity.ok(CustomerResourceFromEntityAssembler.toResourceFromEntity(customer));
                })
                .orElseGet(() -> {
                    System.out.println("❌ Cliente no encontrado");
                    return ResponseEntity.notFound().build();
                });
    }

    @Operation(summary = "Obtener configuración de visualización del tenant",
               description = "Retorna la configuración de cómo mostrar los datos del cliente según el tenant")
    @ApiResponse(responseCode = "200", description = "Configuración obtenida exitosamente")
    @GetMapping("/display-config/{tenantCode}")
    public ResponseEntity<?> getDisplayConfig(
            @Parameter(description = "Código del tenant", example = "FIN-OH") @PathVariable String tenantCode) {

        System.out.println("📋 Obteniendo configuración de visualización para: " + tenantCode);

        try {
            String configPath = "tenant-configurations/" + tenantCode.toLowerCase() + ".json";
            ClassPathResource resource = new ClassPathResource(configPath);

            if (!resource.exists()) {
                System.out.println("❌ No se encontró archivo de configuración: " + configPath);
                return ResponseEntity.notFound().build();
            }

            JsonNode rootNode = objectMapper.readTree(resource.getInputStream());
            JsonNode displayConfigNode = rootNode.path("customerDataMapping").path("customerDisplayConfig");

            if (displayConfigNode.isMissingNode()) {
                System.out.println("❌ No se encontró customerDisplayConfig en la configuración");
                return ResponseEntity.notFound().build();
            }

            CustomerDataMapping.CustomerDisplayConfig displayConfig =
                objectMapper.treeToValue(displayConfigNode, CustomerDataMapping.CustomerDisplayConfig.class);

            System.out.println("✅ Configuración cargada: " + displayConfig.getSections().size() + " secciones");
            return ResponseEntity.ok(displayConfig);

        } catch (Exception e) {
            System.err.println("❌ Error al cargar configuración: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Error al cargar configuración: " + e.getMessage());
        }
    }
}
