package com.cashi.customermanagement.interfaces.rest.controllers;

// import com.cashi.customermanagement.application.internal.commandservices.CustomerSyncService;
// import com.cashi.customermanagement.application.internal.queryservices.CustomerDetailQueryService;
import com.cashi.customermanagement.application.internal.queryservices.CustomerQueryServiceImpl;
import com.cashi.customermanagement.domain.model.valueobjects.CustomerDataMapping;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerRepository;
// import com.cashi.customermanagement.interfaces.rest.resources.CustomerDetailResource;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "Gestión de clientes, cuentas y deudas")
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerQueryServiceImpl queryService;
    // private final CustomerDetailQueryService customerDetailQueryService;
    private final com.cashi.customermanagement.application.internal.commandservices.CustomerSyncService customerSyncService;
    private final CustomerRepository customerRepository;
    private final com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ContactMethodRepository contactMethodRepository;
    private final ObjectMapper objectMapper;
    private final CustomerResourceFromEntityAssembler assembler;

    public CustomerController(CustomerQueryServiceImpl queryService,
                            // CustomerDetailQueryService customerDetailQueryService,
                            com.cashi.customermanagement.application.internal.commandservices.CustomerSyncService customerSyncService,
                            CustomerRepository customerRepository,
                            com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ContactMethodRepository contactMethodRepository,
                            ObjectMapper objectMapper,
                            CustomerResourceFromEntityAssembler assembler) {
        this.queryService = queryService;
        // this.customerDetailQueryService = customerDetailQueryService;
        this.customerSyncService = customerSyncService;
        this.customerRepository = customerRepository;
        this.contactMethodRepository = contactMethodRepository;
        this.objectMapper = objectMapper;
        this.assembler = assembler;
    }

    @Operation(summary = "Obtener todos los clientes", description = "Retorna lista completa de clientes con su información de cuenta y deuda")
    @ApiResponse(responseCode = "200", description = "Lista de clientes obtenida exitosamente")
    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<CustomerResource>> getAllCustomers() {
        var customers = queryService.getAllCustomers();
        var resources = customers.stream()
                .map(assembler::toResourceFromEntity)
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
                        assembler.toResourceFromEntity(customer)))
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
                        assembler.toResourceFromEntity(customer)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Buscar clientes", description = "Busca clientes por nombre, documento o cuenta")
    @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos")
    @GetMapping("/search")
    public ResponseEntity<List<CustomerResource>> searchCustomers(
            @Parameter(description = "Término de búsqueda") @RequestParam String query) {
        var customers = queryService.searchCustomers(query);
        var resources = customers.stream()
                .map(assembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Buscar cliente por criterio específico",
               description = "Busca un cliente por un criterio específico: codigo_identificacion, documento, numero_cuenta, telefono_principal")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
        @ApiResponse(responseCode = "400", description = "Criterio de búsqueda inválido")
    })
    @GetMapping("/search-by")
    public ResponseEntity<?> searchCustomerByCriteria(
            @Parameter(description = "ID del tenant", required = true) @RequestParam Long tenantId,
            @Parameter(description = "Criterio de búsqueda: codigo_identificacion, documento, numero_cuenta, telefono_principal",
                       required = true, example = "codigo_identificacion") @RequestParam String searchBy,
            @Parameter(description = "Valor a buscar", required = true, example = "D000007530354") @RequestParam String value) {

        System.out.println("🔍 Búsqueda: tenantId=" + tenantId + ", searchBy=" + searchBy + ", value=" + value);

        // Buscar según el criterio especificado
        return switch (searchBy.toLowerCase()) {
            case "codigo_identificacion" -> customerRepository.findByTenantIdAndIdentificationCodeWithContactMethods(tenantId, value)
                    .map(customer -> {
                        System.out.println("✅ Cliente encontrado por codigo_identificacion: " + customer.getFullName());
                        return ResponseEntity.ok(assembler.toResourceFromEntity(customer));
                    })
                    .orElseGet(() -> {
                        System.out.println("❌ Cliente no encontrado por codigo_identificacion");
                        return ResponseEntity.notFound().build();
                    });

            case "documento" -> customerRepository.findByTenantIdAndDocumentWithContactMethods(tenantId, value)
                    .map(customer -> {
                        System.out.println("✅ Cliente encontrado por documento: " + customer.getFullName());
                        return ResponseEntity.ok(assembler.toResourceFromEntity(customer));
                    })
                    .orElseGet(() -> {
                        System.out.println("❌ Cliente no encontrado por documento");
                        return ResponseEntity.notFound().build();
                    });

            case "telefono_principal" -> contactMethodRepository.findByTenantIdAndSubtypeAndValueWithCustomer(tenantId, "telefono_principal", value)
                    .map(contactMethod -> {
                        System.out.println("✅ Cliente encontrado por telefono_principal: " + contactMethod.getCustomer().getFullName());
                        return ResponseEntity.ok(assembler.toResourceFromEntity(contactMethod.getCustomer()));
                    })
                    .orElseGet(() -> {
                        System.out.println("❌ Cliente no encontrado por telefono_principal");
                        return ResponseEntity.notFound().build();
                    });

            case "numero_cuenta" -> customerRepository.findByTenantIdAndAccountNumberWithContactMethods(tenantId, value)
                    .map(customer -> {
                        System.out.println("✅ Cliente encontrado por numero_cuenta: " + customer.getFullName());
                        return ResponseEntity.ok(assembler.toResourceFromEntity(customer));
                    })
                    .orElseGet(() -> {
                        System.out.println("❌ Cliente no encontrado por numero_cuenta");
                        return ResponseEntity.notFound().build();
                    });

            default -> {
                System.out.println("❌ Criterio de búsqueda inválido: " + searchBy);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Criterio de búsqueda inválido");
                errorResponse.put("message", "Criterios válidos: codigo_identificacion, documento, numero_cuenta, telefono_principal");
                yield ResponseEntity.badRequest().body(errorResponse);
            }
        };
    }

    @Operation(summary = "Buscar múltiples clientes por criterio específico",
               description = "Busca todos los clientes que coincidan con un criterio específico, retornando múltiples resultados si existen en diferentes subcarteras")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Clientes encontrados (puede ser lista vacía)"),
        @ApiResponse(responseCode = "400", description = "Criterio de búsqueda inválido")
    })
    @GetMapping("/search-by-multi")
    public ResponseEntity<?> searchCustomersByCriteria(
            @Parameter(description = "ID del tenant", required = true) @RequestParam Long tenantId,
            @Parameter(description = "Criterio de búsqueda: codigo_identificacion, documento, numero_cuenta, telefono_principal",
                       required = true, example = "documento") @RequestParam String searchBy,
            @Parameter(description = "Valor a buscar", required = true, example = "12345678") @RequestParam String value) {

        System.out.println("🔍 Búsqueda múltiple: tenantId=" + tenantId + ", searchBy=" + searchBy + ", value=" + value);

        // Buscar según el criterio especificado, retornando lista
        return switch (searchBy.toLowerCase()) {
            case "codigo_identificacion" -> {
                var customers = customerRepository.findAllByTenantIdAndIdentificationCodeWithContactMethods(tenantId, value);
                System.out.println("✅ Encontrados " + customers.size() + " clientes por codigo_identificacion");
                var resources = customers.stream()
                        .map(assembler::toResourceFromEntity)
                        .toList();
                yield ResponseEntity.ok(resources);
            }

            case "documento" -> {
                var customers = customerRepository.findAllByTenantIdAndDocumentWithContactMethods(tenantId, value);
                System.out.println("✅ Encontrados " + customers.size() + " clientes por documento");
                var resources = customers.stream()
                        .map(assembler::toResourceFromEntity)
                        .toList();
                yield ResponseEntity.ok(resources);
            }

            case "telefono_principal" -> {
                var contactMethods = contactMethodRepository.findAllByTenantIdAndSubtypeAndValueWithCustomer(tenantId, "telefono_principal", value);
                System.out.println("✅ Encontrados " + contactMethods.size() + " clientes por telefono_principal");
                var resources = contactMethods.stream()
                        .map(cm -> assembler.toResourceFromEntity(cm.getCustomer()))
                        .toList();
                yield ResponseEntity.ok(resources);
            }

            case "numero_cuenta" -> {
                var customers = customerRepository.findAllByTenantIdAndAccountNumberWithContactMethods(tenantId, value);
                System.out.println("✅ Encontrados " + customers.size() + " clientes por numero_cuenta");
                var resources = customers.stream()
                        .map(assembler::toResourceFromEntity)
                        .toList();
                yield ResponseEntity.ok(resources);
            }

            default -> {
                System.out.println("❌ Criterio de búsqueda inválido: " + searchBy);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Criterio de búsqueda inválido");
                errorResponse.put("message", "Criterios válidos: codigo_identificacion, documento, numero_cuenta, telefono_principal");
                yield ResponseEntity.badRequest().body(errorResponse);
            }
        };
    }

    @Operation(summary = "Buscar clientes en TODOS los tenants (búsqueda multi-tenant global)",
               description = "Busca clientes que coincidan con el criterio SIN filtrar por tenant. Útil para encontrar duplicados entre diferentes inquilinos.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Clientes encontrados (puede ser lista vacía)"),
        @ApiResponse(responseCode = "400", description = "Criterio de búsqueda inválido")
    })
    @GetMapping("/search-all-tenants")
    public ResponseEntity<?> searchCustomersAcrossAllTenants(
            @Parameter(description = "Criterio de búsqueda: documento, numero_cuenta, telefono_principal",
                       required = true, example = "documento") @RequestParam String searchBy,
            @Parameter(description = "Valor a buscar", required = true, example = "12345678") @RequestParam String value) {

        System.out.println("🔍 Búsqueda multi-tenant GLOBAL: searchBy=" + searchBy + ", value=" + value);

        // Buscar según el criterio especificado SIN filtro de tenantId
        return switch (searchBy.toLowerCase()) {
            case "documento" -> {
                var customers = customerRepository.findAllByDocumentWithContactMethods(value);
                System.out.println("✅ Encontrados " + customers.size() + " clientes por documento en todos los tenants");
                var resources = customers.stream()
                        .map(assembler::toResourceFromEntity)
                        .toList();
                yield ResponseEntity.ok(resources);
            }

            case "telefono_principal" -> {
                var contactMethods = contactMethodRepository.findAllBySubtypeAndValueWithCustomer("telefono_principal", value);
                System.out.println("✅ Encontrados " + contactMethods.size() + " clientes por telefono_principal en todos los tenants");
                var resources = contactMethods.stream()
                        .map(cm -> assembler.toResourceFromEntity(cm.getCustomer()))
                        .toList();
                yield ResponseEntity.ok(resources);
            }

            case "numero_cuenta" -> {
                var customers = customerRepository.findAllByAccountNumberWithContactMethods(value);
                System.out.println("✅ Encontrados " + customers.size() + " clientes por numero_cuenta en todos los tenants");
                var resources = customers.stream()
                        .map(assembler::toResourceFromEntity)
                        .toList();
                yield ResponseEntity.ok(resources);
            }

            default -> {
                System.out.println("❌ Criterio de búsqueda inválido: " + searchBy);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Criterio de búsqueda inválido");
                errorResponse.put("message", "Criterios válidos: documento, numero_cuenta, telefono_principal");
                yield ResponseEntity.badRequest().body(errorResponse);
            }
        };
    }

    @Operation(summary = "Obtener clientes más recientes", description = "Retorna los últimos 6 clientes buscados")
    @ApiResponse(responseCode = "200", description = "Lista de clientes recientes obtenida")
    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, String>>> getRecentCustomers() {
        var customers = customerRepository.findTop6ByLastAccessedAtNotNullOrderByLastAccessedAtDesc();
        var simplified = customers.stream()
                .map(customer -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("document", customer.getDocument());
                    map.put("fullName", customer.getFullName());
                    map.put("tenantName", customer.getTenantName() != null ? customer.getTenantName() : "N/A");
                    map.put("portfolioName", customer.getPortfolioName() != null ? customer.getPortfolioName() : "N/A");
                    map.put("subPortfolioName", customer.getSubPortfolioName() != null ? customer.getSubPortfolioName() : "N/A");
                    return map;
                })
                .toList();
        return ResponseEntity.ok(simplified);
    }

    @Operation(summary = "Registrar acceso a cliente", description = "Actualiza la fecha de último acceso del cliente")
    @ApiResponse(responseCode = "200", description = "Acceso registrado exitosamente")
    @PostMapping("/{customerId}/access")
    public ResponseEntity<Void> registerCustomerAccess(
            @Parameter(description = "ID del cliente", required = true) @PathVariable Long customerId) {
        var customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isPresent()) {
            var customer = customerOpt.get();
            customer.updateLastAccessedAt();
            customerRepository.save(customer);
            System.out.println("✅ Acceso registrado para cliente ID: " + customerId);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Obtener cliente por código de identificación y tenant",
               description = "Busca un cliente por su código de identificación dentro de un tenant específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/by-document")
    public ResponseEntity<CustomerResource> getCustomerByIdentificationCode(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Código de identificación", example = "COD001") @RequestParam String identificationCode) {

        System.out.println("🔍 Buscando cliente: tenantId=" + tenantId + ", identificationCode=" + identificationCode);

        return customerRepository.findByTenantIdAndIdentificationCode(tenantId, identificationCode)
                .map(customer -> {
                    System.out.println("✅ Cliente encontrado: " + customer.getFullName());
                    return ResponseEntity.ok(assembler.toResourceFromEntity(customer));
                })
                .orElseGet(() -> {
                    System.out.println("❌ Cliente no encontrado");
                    return ResponseEntity.notFound().build();
                });
    }

    // TEMPORARILY DISABLED - Needs refactoring to match actual entities
    /*
    @Operation(summary = "Obtener detalle completo del cliente con mapeo de campos dinámico",
               description = "Retorna información completa del cliente con etiquetas de campos según configuración de sub-cartera")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalle del cliente obtenido exitosamente"),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/detail/{document}")
    public ResponseEntity<CustomerDetailResource> getCustomerDetail(
            @Parameter(description = "Número de documento del cliente", example = "12345678") @PathVariable String document,
            @Parameter(description = "ID de la sub-cartera", example = "1") @RequestParam Long subPortfolioId) {

        return customerDetailQueryService.searchCustomerByDocument(document, subPortfolioId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    */

    @Operation(summary = "Sincronizar clientes desde tabla dinámica de carga inicial",
               description = "Lee clientes de la tabla dinámica ini_<prov>_<car>_<subcartera> y los sincroniza a la tabla clientes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sincronización completada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en sincronización")
    })
    @PostMapping("/sync/{subPortfolioId}")
    public ResponseEntity<?> syncCustomers(
            @Parameter(description = "ID de la sub-cartera", example = "1") @PathVariable Long subPortfolioId) {

        System.out.println("🔄 POST /api/v1/customers/sync/" + subPortfolioId);

        try {
            com.cashi.customermanagement.application.internal.commandservices.CustomerSyncService.SyncResult result =
                    customerSyncService.syncCustomersFromSubPortfolio(subPortfolioId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("customersCreated", result.getCustomersCreated());
            response.put("customersUpdated", result.getCustomersUpdated());
            response.put("totalCustomers", result.getTotalCustomers());
            response.put("hasErrors", result.hasErrors());
            response.put("errors", result.getErrors());

            String message = String.format("Sincronización completada: %d clientes creados, %d actualizados",
                    result.getCustomersCreated(), result.getCustomersUpdated());
            response.put("message", message);

            System.out.println("✅ " + message);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error en sincronización: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error en sincronización: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Sincronizar clientes desde tabla específica (método directo)",
               description = "Lee clientes de una tabla dinámica especificada por nombre y los sincroniza")
    @PostMapping("/sync/table/{tableName}/{tenantId}")
    public ResponseEntity<?> syncCustomersFromTable(
            @Parameter(description = "Nombre de la tabla", example = "ini_t01_pp_con") @PathVariable String tableName,
            @Parameter(description = "ID del tenant", example = "1") @PathVariable Long tenantId) {

        System.out.println("🔄 POST /api/v1/customers/sync/table/" + tableName + "/" + tenantId);

        try {
            com.cashi.customermanagement.application.internal.commandservices.CustomerSyncService.SyncResult result =
                    customerSyncService.syncCustomersFromTable(tableName, tenantId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("customersCreated", result.getCustomersCreated());
            response.put("customersUpdated", result.getCustomersUpdated());
            response.put("totalCustomers", result.getTotalCustomers());
            response.put("hasErrors", result.hasErrors());
            response.put("errors", result.getErrors());

            String message = String.format("Sincronización completada: %d clientes creados, %d actualizados",
                    result.getCustomersCreated(), result.getCustomersUpdated());
            response.put("message", message);

            System.out.println("✅ " + message);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error en sincronización: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error en sincronización: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
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
