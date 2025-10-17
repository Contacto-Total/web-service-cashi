package com.cashi.customermanagement.interfaces.rest.controllers;

import com.cashi.customermanagement.application.internal.commandservices.CustomerImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller para importación de clientes desde Excel/CSV
 */
@Tag(name = "Customer Import", description = "Endpoints para importar clientes desde archivos")
@RestController
@RequestMapping("/api/v1/customers/import")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CustomerImportController {

    private final CustomerImportService customerImportService;

    @Operation(summary = "Importar clientes desde archivo Excel o CSV")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importCustomers(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tenantId") Long tenantId,
            @RequestParam("tenantCode") String tenantCode) {

        System.out.println("📤 POST /api/v1/customers/import");
        System.out.println("   - tenantId: " + tenantId);
        System.out.println("   - tenantCode: " + tenantCode);
        System.out.println("   - fileName: " + file.getOriginalFilename());
        System.out.println("   - fileSize: " + file.getSize() + " bytes");

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("El archivo está vacío"));
        }

        try {
            CustomerImportService.ImportResult result =
                    customerImportService.importCustomers(tenantId, file, tenantCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("importedCount", result.getSuccessCount());
            response.put("hasErrors", result.hasErrors());
            response.put("errors", result.getErrors());
            response.put("message", result.getSuccessCount() + " clientes importados exitosamente");

            if (result.hasErrors()) {
                response.put("message", result.getSuccessCount() + " clientes importados con " +
                        result.getErrors().size() + " errores");
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            System.err.println("❌ Error en importación: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error al importar clientes: " + e.getMessage()));
        }
    }

    @Operation(summary = "Obtener configuración de mapeo del tenant")
    @GetMapping("/config/{tenantCode}")
    public ResponseEntity<Map<String, Object>> getTenantConfig(@PathVariable String tenantCode) {
        System.out.println("📤 GET /api/v1/customers/import/config/" + tenantCode);

        try {
            // Aquí podrías devolver la configuración del tenant si lo necesitas
            Map<String, Object> response = new HashMap<>();
            response.put("tenantCode", tenantCode);
            response.put("message", "Para ver la configuración completa, consulta el archivo: tenant-configurations/" +
                    tenantCode.toLowerCase() + ".json");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error al obtener configuración: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
