package com.cashi.systemconfiguration.interfaces.rest.controllers;

import com.cashi.shared.domain.model.valueobjects.LoadType;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.FieldDefinitionRepository;
import com.cashi.systemconfiguration.domain.services.HeaderConfigurationCommandService;
import com.cashi.systemconfiguration.domain.services.HeaderConfigurationQueryService;
import com.cashi.systemconfiguration.interfaces.rest.resources.*;
import com.cashi.systemconfiguration.interfaces.rest.transform.HeaderConfigurationResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/system-config/header-configurations")
@Tag(name = "Header Configuration", description = "Configuración de Cabeceras Personalizadas")
public class HeaderConfigurationController {

    private final HeaderConfigurationCommandService commandService;
    private final HeaderConfigurationQueryService queryService;
    private final FieldDefinitionRepository fieldDefinitionRepository;

    public HeaderConfigurationController(
            HeaderConfigurationCommandService commandService,
            HeaderConfigurationQueryService queryService,
            FieldDefinitionRepository fieldDefinitionRepository) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.fieldDefinitionRepository = fieldDefinitionRepository;
    }

    @Operation(summary = "Obtener configuraciones de cabeceras por subcartera")
    @GetMapping("/subportfolio/{subPortfolioId}")
    public ResponseEntity<List<HeaderConfigurationResource>> getBySubPortfolio(
            @PathVariable Integer subPortfolioId) {
        try {
            var configs = queryService.getAllBySubPortfolio(subPortfolioId);
            var resources = configs.stream()
                    .map(HeaderConfigurationResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();
            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Obtener configuraciones de cabeceras por subcartera y tipo de carga")
    @GetMapping("/subportfolio/{subPortfolioId}/load-type/{loadType}")
    public ResponseEntity<List<HeaderConfigurationResource>> getBySubPortfolioAndLoadType(
            @PathVariable Integer subPortfolioId,
            @PathVariable LoadType loadType) {
        try {
            var configs = queryService.getAllBySubPortfolioAndLoadType(subPortfolioId, loadType);
            var resources = configs.stream()
                    .map(HeaderConfigurationResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();
            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Obtener configuración de cabecera por ID")
    @GetMapping("/{id}")
    public ResponseEntity<HeaderConfigurationResource> getById(@PathVariable Integer id) {
        var config = queryService.getById(id);
        return config.map(c -> ResponseEntity.ok(
                HeaderConfigurationResourceFromEntityAssembler.toResourceFromEntity(c)
        )).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear nueva configuración de cabecera")
    @PostMapping
    public ResponseEntity<HeaderConfigurationResource> create(
            @RequestBody CreateHeaderConfigurationResource resource) {
        try {
            var config = commandService.createHeaderConfiguration(
                    resource.subPortfolioId(),
                    resource.fieldDefinitionId(),
                    resource.headerName(),
                    resource.displayLabel(),
                    resource.format(),
                    resource.required(),
                    resource.loadType()
            );
            var responseResource = HeaderConfigurationResourceFromEntityAssembler.toResourceFromEntity(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Crear múltiples configuraciones en lote (importar CSV)")
    @PostMapping("/bulk")
    public ResponseEntity<?> createBulk(
            @RequestBody BulkCreateHeaderConfigurationResource resource) {
        try {
            System.out.println("📥 Recibiendo bulk create: subPortfolioId=" + resource.subPortfolioId()
                    + ", loadType=" + resource.loadType()
                    + ", headers=" + resource.headers().size());

            var dataList = resource.headers().stream()
                    .map(h -> new HeaderConfigurationCommandService.HeaderConfigurationData(
                            h.fieldDefinitionId(),
                            h.headerName(),
                            h.dataType(),
                            h.displayLabel(),
                            h.format(),
                            h.required(),
                            h.sourceField(),
                            h.regexPattern()
                    ))
                    .toList();

            var configs = commandService.createBulkHeaderConfigurations(
                    resource.subPortfolioId(),
                    resource.loadType(),
                    dataList
            );

            var resources = configs.stream()
                    .map(HeaderConfigurationResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();

            System.out.println("✅ Bulk create exitoso: " + resources.size() + " configuraciones creadas");
            return ResponseEntity.status(HttpStatus.CREATED).body(resources);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error en bulk create: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error inesperado en bulk create: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar configuración de cabecera")
    @PutMapping("/{id}")
    public ResponseEntity<HeaderConfigurationResource> update(
            @PathVariable Integer id,
            @RequestBody UpdateHeaderConfigurationResource resource) {
        try {
            var config = commandService.updateHeaderConfiguration(
                    id,
                    resource.displayLabel(),
                    resource.format(),
                    resource.required(),
                    resource.loadType()
            );
            var responseResource = HeaderConfigurationResourceFromEntityAssembler.toResourceFromEntity(config);
            return ResponseEntity.ok(responseResource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Eliminar configuración de cabecera")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            commandService.deleteHeaderConfiguration(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Eliminar todas las configuraciones de una subcartera y tipo de carga")
    @DeleteMapping("/subportfolio/{subPortfolioId}/load-type/{loadType}")
    public ResponseEntity<Void> deleteAllBySubPortfolioAndLoadType(
            @PathVariable Integer subPortfolioId,
            @PathVariable LoadType loadType) {
        try {
            commandService.deleteAllBySubPortfolioAndLoadType(subPortfolioId, loadType);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Contar configuraciones de una subcartera")
    @GetMapping("/subportfolio/{subPortfolioId}/count")
    public ResponseEntity<Long> countBySubPortfolio(@PathVariable Integer subPortfolioId) {
        try {
            var count = queryService.countBySubPortfolio(subPortfolioId);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Importar configuraciones de cabeceras desde archivo CSV")
    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importFromCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subPortfolioId") Integer subPortfolioId,
            @RequestParam("loadType") LoadType loadType) {

        System.out.println("📤 POST /api/v1/system-config/header-configurations/import-csv");
        System.out.println("   - subPortfolioId: " + subPortfolioId);
        System.out.println("   - loadType: " + loadType);
        System.out.println("   - fileName: " + file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo está vacío"));
        }

        try {
            List<BulkCreateHeaderConfigurationResource.HeaderConfigurationItem> headers = new ArrayList<>();

            // Leer el CSV
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                boolean isFirstLine = true;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    // Saltar la primera línea (cabecera)
                    if (isFirstLine) {
                        isFirstLine = false;
                        System.out.println("Saltando cabecera CSV: " + line);
                        continue;
                    }

                    // Saltar líneas vacías
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    // Parsear la línea: codigoCampo;nombreCabecera;etiquetaVisual;formato;tipoDato;obligatorio;campoAsociado;regEx
                    String[] parts = line.split(";", -1); // -1 para mantener campos vacíos

                    if (parts.length < 6) {
                        System.err.println("⚠️ Línea " + lineNumber + " tiene menos de 6 campos, saltando: " + line);
                        continue;
                    }

                    String codigoCampo = parts[0].trim();
                    String nombreCabecera = parts[1].trim();
                    String etiquetaVisual = parts[2].trim();
                    String formato = parts.length > 3 ? parts[3].trim() : "";
                    String tipoDato = parts.length > 4 ? parts[4].trim().toUpperCase() : "TEXTO";
                    String obligatorioStr = parts.length > 5 ? parts[5].trim() : "0";
                    String campoAsociado = parts.length > 6 ? parts[6].trim() : "";
                    String regEx = parts.length > 7 ? parts[7].trim() : "";

                    // Limpiar las comillas de la regEx si existen
                    if (regEx.startsWith("\"") && regEx.endsWith("\"")) {
                        regEx = regEx.substring(1, regEx.length() - 1);
                    }

                    // Validar campos obligatorios
                    if (nombreCabecera.isEmpty()) {
                        System.err.println("⚠️ Línea " + lineNumber + " sin nombreCabecera, saltando");
                        continue;
                    }

                    // Buscar fieldDefinitionId si existe codigoCampo
                    Integer fieldDefinitionId = null;
                    if (!codigoCampo.isEmpty()) {
                        var fieldDefinition = fieldDefinitionRepository.findByFieldCode(codigoCampo);
                        if (fieldDefinition.isPresent()) {
                            fieldDefinitionId = fieldDefinition.get().getId();
                            System.out.println("✓ Campo " + codigoCampo + " -> fieldDefinitionId: " + fieldDefinitionId);
                        } else {
                            System.out.println("⚠️ Campo " + codigoCampo + " no encontrado en catálogo, será campo personalizado");
                        }
                    }

                    // Convertir obligatorio a Boolean
                    Boolean required = "1".equals(obligatorioStr) || "true".equalsIgnoreCase(obligatorioStr);

                    // Validar formato si es vacío
                    if (formato.isEmpty()) {
                        formato = null;
                    }

                    // Validar campoAsociado y regEx
                    if (campoAsociado.isEmpty()) {
                        campoAsociado = null;
                    }
                    if (regEx.isEmpty()) {
                        regEx = null;
                    }

                    // Crear el item
                    var item = new BulkCreateHeaderConfigurationResource.HeaderConfigurationItem(
                        fieldDefinitionId,
                        nombreCabecera,
                        tipoDato,
                        etiquetaVisual,
                        formato,
                        required,
                        campoAsociado,
                        regEx
                    );

                    headers.add(item);
                    System.out.println("✓ Línea " + lineNumber + ": " + nombreCabecera +
                        " (field=" + fieldDefinitionId + ", tipo=" + tipoDato +
                        ", req=" + required + ", source=" + campoAsociado +
                        ", regex=" + (regEx != null ? "✓" : "✗") + ")");
                }
            }

            if (headers.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No se encontraron cabeceras válidas en el archivo CSV"));
            }

            System.out.println("\n📋 Total de cabeceras parseadas: " + headers.size());

            // Crear las configuraciones en lote
            var dataList = headers.stream()
                    .map(h -> new HeaderConfigurationCommandService.HeaderConfigurationData(
                            h.fieldDefinitionId(),
                            h.headerName(),
                            h.dataType(),
                            h.displayLabel(),
                            h.format(),
                            h.required(),
                            h.sourceField(),
                            h.regexPattern()
                    ))
                    .toList();

            var configs = commandService.createBulkHeaderConfigurations(
                    subPortfolioId,
                    loadType,
                    dataList
            );

            var resources = configs.stream()
                    .map(HeaderConfigurationResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();

            System.out.println("✅ Importación exitosa: " + resources.size() + " configuraciones creadas");

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Configuraciones creadas exitosamente",
                "count", resources.size(),
                "configurations", resources
            ));

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error al procesar CSV: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar CSV: " + e.getMessage()));
        }
    }

    @Operation(summary = "Importar datos masivos a la tabla dinámica")
    @PostMapping("/subportfolio/{subPortfolioId}/import-data")
    public ResponseEntity<?> importData(
            @PathVariable Integer subPortfolioId,
            @RequestBody ImportDataResource resource) {
        try {
            var result = commandService.importDataToTable(subPortfolioId, resource.loadType(), resource.data());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al importar datos: " + e.getMessage()));
        }
    }
}
