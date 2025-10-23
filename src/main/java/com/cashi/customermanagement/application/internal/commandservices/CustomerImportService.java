package com.cashi.customermanagement.application.internal.commandservices;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import com.cashi.customermanagement.domain.model.valueobjects.CustomerDataMapping;
import com.cashi.customermanagement.domain.model.valueobjects.DocumentNumber;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Servicio para importar clientes desde Excel/CSV con mapeo configurable por tenant
 */
@Service
@RequiredArgsConstructor
public class CustomerImportService {

    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    /**
     * Importa clientes desde un archivo Excel/CSV usando la configuración del tenant
     */
    @Transactional
    public ImportResult importCustomers(Long tenantId, MultipartFile file, String tenantCode) throws IOException {
        System.out.println("📥 Iniciando importación de clientes para tenant: " + tenantCode);

        // Cargar configuración del tenant
        CustomerDataMapping mapping = loadTenantConfiguration(tenantCode);
        if (mapping == null) {
            throw new IllegalArgumentException("No se encontró configuración para el tenant: " + tenantCode);
        }

        List<Customer> importedCustomers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Nombre de archivo no válido");
        }

        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                importedCustomers = importFromExcel(tenantId, file.getInputStream(), mapping, errors);
            } else if (filename.endsWith(".csv")) {
                importedCustomers = importFromCSV(tenantId, file.getInputStream(), mapping, errors);
            } else {
                throw new IllegalArgumentException("Formato de archivo no soportado. Use .xlsx, .xls o .csv");
            }

            // Guardar todos los clientes importados
            customerRepository.saveAll(importedCustomers);

            System.out.println("✅ Importación completada: " + importedCustomers.size() + " clientes importados");
            if (!errors.isEmpty()) {
                System.out.println("⚠️ Errores encontrados: " + errors.size());
                errors.forEach(error -> System.out.println("   - " + error));
            }

            return new ImportResult(importedCustomers.size(), errors);

        } catch (Exception e) {
            System.err.println("❌ Error en importación: " + e.getMessage());
            throw new RuntimeException("Error al importar clientes: " + e.getMessage(), e);
        }
    }

    /**
     * Importa clientes desde archivo Excel
     */
    private List<Customer> importFromExcel(Long tenantId, InputStream inputStream,
                                          CustomerDataMapping mapping, List<String> errors) throws IOException {
        List<Customer> customers = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Obtener encabezados de la primera fila
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("El archivo no tiene encabezados");
            }

            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (Cell cell : headerRow) {
                columnIndexMap.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }

            // Procesar cada fila
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Map<String, String> rowData = new HashMap<>();
                    for (Map.Entry<String, Integer> entry : columnIndexMap.entrySet()) {
                        Cell cell = row.getCell(entry.getValue());
                        rowData.put(entry.getKey(), getCellValueAsString(cell));
                    }

                    Customer customer = mapToCustomer(tenantId, rowData, mapping);
                    if (customer != null) {
                        customers.add(customer);
                    }
                } catch (Exception e) {
                    errors.add("Fila " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        return customers;
    }

    /**
     * Importa clientes desde archivo CSV
     */
    private List<Customer> importFromCSV(Long tenantId, InputStream inputStream,
                                        CustomerDataMapping mapping, List<String> errors) throws IOException {
        List<Customer> customers = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withTrim()
                .parse(new InputStreamReader(inputStream))) {

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                try {
                    Map<String, String> rowData = record.toMap();
                    Customer customer = mapToCustomer(tenantId, rowData, mapping);
                    if (customer != null) {
                        customers.add(customer);
                    }
                } catch (Exception e) {
                    errors.add("Fila " + rowNum + ": " + e.getMessage());
                }
            }
        }

        return customers;
    }

    /**
     * Mapea una fila de datos a un objeto Customer usando la configuración del tenant
     */
    private Customer mapToCustomer(Long tenantId, Map<String, String> rowData, CustomerDataMapping mapping) {
        Map<String, String> columnMapping = mapping.getColumnMapping();

        // Obtener valores mapeados
        String documentCode = getMappedValue(rowData, columnMapping, "documentCode");
        String fullName = getMappedValue(rowData, columnMapping, "fullName");
        String status = getMappedValue(rowData, columnMapping, "status", "ACTIVO");

        if (documentCode == null || documentCode.isEmpty()) {
            throw new IllegalArgumentException("Documento requerido");
        }
        if (fullName == null || fullName.isEmpty()) {
            throw new IllegalArgumentException("Nombre completo requerido");
        }

        // Verificar si el cliente ya existe
        Optional<Customer> existingCustomer = customerRepository.findByTenantIdAndDocumentCode(tenantId, documentCode);
        if (existingCustomer.isPresent()) {
            System.out.println("⚠️ Cliente ya existe, actualizando: " + documentCode);
            return updateExistingCustomer(existingCustomer.get(), rowData, columnMapping);
        }

        // Crear nuevo cliente
        String customerId = UUID.randomUUID().toString();
        DocumentNumber documentNumber = new DocumentNumber("DNI", documentCode);

        Customer customer = new Customer(tenantId, customerId, documentCode, fullName,
                                        documentNumber, null, status);

        // ContactInfo, AccountInfo, and DebtInfo have been removed

        return customer;
    }

    /**
     * Actualiza un cliente existente con nuevos datos
     */
    private Customer updateExistingCustomer(Customer customer, Map<String, String> rowData,
                                           Map<String, String> columnMapping) {
        // ContactInfo, AccountInfo, and DebtInfo have been removed
        return customer;
    }

    /**
     * Crea ContactInfo desde los datos mapeados
     * DEPRECATED: ContactInfo entity has been removed
     */
    private Object createContactInfo(Map<String, String> rowData, Map<String, String> columnMapping) {
        return null;
    }

    /**
     * Crea AccountInfo desde los datos mapeados
     * DEPRECATED: AccountInfo entity has been removed
     */
    private Object createAccountInfo(Map<String, String> rowData, Map<String, String> columnMapping) {
        return null;
    }

    /**
     * Crea DebtInfo desde los datos mapeados
     * DEPRECATED: DebtInfo entity has been removed
     */
    private Object createDebtInfo(Map<String, String> rowData, Map<String, String> columnMapping) {
        return null;
    }

    /**
     * Obtiene un valor mapeado desde los datos de la fila
     */
    private String getMappedValue(Map<String, String> rowData, Map<String, String> columnMapping,
                                  String targetField) {
        return getMappedValue(rowData, columnMapping, targetField, null);
    }

    /**
     * Obtiene un valor mapeado desde los datos de la fila con valor por defecto
     */
    private String getMappedValue(Map<String, String> rowData, Map<String, String> columnMapping,
                                  String targetField, String defaultValue) {
        // Buscar la columna origen que mapea a este campo destino
        for (Map.Entry<String, String> entry : columnMapping.entrySet()) {
            if (entry.getValue().equals(targetField)) {
                String value = rowData.get(entry.getKey());
                return (value != null && !value.isEmpty()) ? value : defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Convierte el valor de una celda a String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Parsea una fecha en formato ISO
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            System.err.println("⚠️ Error parseando fecha: " + value);
            return null;
        }
    }

    /**
     * Parsea un BigDecimal
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (Exception e) {
            System.err.println("⚠️ Error parseando número: " + value);
            return null;
        }
    }

    /**
     * Parsea un Integer
     */
    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Integer.parseInt(value.replace(",", "").split("\\.")[0]);
        } catch (Exception e) {
            System.err.println("⚠️ Error parseando entero: " + value);
            return null;
        }
    }

    /**
     * Carga la configuración del tenant desde el archivo JSON
     */
    private CustomerDataMapping loadTenantConfiguration(String tenantCode) {
        try {
            String configPath = "tenant-configurations/" + tenantCode.toLowerCase() + ".json";
            ClassPathResource resource = new ClassPathResource(configPath);

            if (!resource.exists()) {
                System.err.println("❌ No se encontró archivo de configuración: " + configPath);
                return null;
            }

            JsonNode rootNode = objectMapper.readTree(resource.getInputStream());
            JsonNode mappingNode = rootNode.get("customerDataMapping");

            if (mappingNode == null) {
                System.err.println("❌ No se encontró 'customerDataMapping' en la configuración");
                return null;
            }

            return objectMapper.treeToValue(mappingNode, CustomerDataMapping.class);

        } catch (Exception e) {
            System.err.println("❌ Error cargando configuración del tenant: " + e.getMessage());
            return null;
        }
    }

    /**
     * Resultado de la importación
     */
    public static class ImportResult {
        private final int successCount;
        private final List<String> errors;

        public ImportResult(int successCount, List<String> errors) {
            this.successCount = successCount;
            this.errors = errors;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
