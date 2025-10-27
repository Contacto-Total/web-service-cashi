package com.cashi.customermanagement.application.internal.commandservices;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Servicio para importar clientes desde Excel/CSV con mapeo configurable por tenant
 */
@Service
@RequiredArgsConstructor
public class CustomerImportService {

    private final CustomerRepository customerRepository;
    private final FieldTransformationService fieldTransformationService;

    /**
     * Importa clientes desde un archivo Excel/CSV usando SOLO configuración de base de datos
     */
    @Transactional
    public ImportResult importCustomers(Long tenantId, MultipartFile file, String tenantCode, Integer subPortfolioId) throws IOException {
        System.out.println("📥 Iniciando importación de clientes para tenant: " + tenantCode);
        if (subPortfolioId == null) {
            throw new IllegalArgumentException("SubPortfolioId es requerido para la importación");
        }
        System.out.println("   SubPortfolio ID: " + subPortfolioId);

        List<Customer> importedCustomers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Nombre de archivo no válido");
        }

        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                importedCustomers = importFromExcel(tenantId, subPortfolioId, file.getInputStream(), errors);
            } else if (filename.endsWith(".csv")) {
                importedCustomers = importFromCSV(tenantId, subPortfolioId, file.getInputStream(), errors);
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
    private List<Customer> importFromExcel(Long tenantId, Integer subPortfolioId, InputStream inputStream,
                                          List<String> errors) throws IOException {
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

                    Customer customer = mapToCustomer(tenantId, subPortfolioId, rowData);
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
    private List<Customer> importFromCSV(Long tenantId, Integer subPortfolioId, InputStream inputStream,
                                        List<String> errors) throws IOException {
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
                    Customer customer = mapToCustomer(tenantId, subPortfolioId, rowData);
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
     * Mapea una fila de datos a un objeto Customer usando SOLO base de datos (sin JSON)
     */
    private Customer mapToCustomer(Long tenantId, Integer subPortfolioId, Map<String, String> rowData) {
        // Aplicar reglas de transformación desde la base de datos
        Map<String, Object> enrichedRowDataObj = new HashMap<>(rowData);
        enrichedRowDataObj = fieldTransformationService.applyTransformationRules(
            enrichedRowDataObj, tenantId, subPortfolioId
        );

        // Buscar directamente el campo "documento" generado por las transformaciones
        String documento = getString(enrichedRowDataObj, "documento");

        // Buscar nombre - puede venir de diferentes columnas
        String fullName = getString(enrichedRowDataObj, "NOMBRE");
        if (fullName == null || fullName.isEmpty()) {
            fullName = getString(enrichedRowDataObj, "nombre_completo");
        }

        if (documento == null || documento.isEmpty()) {
            throw new IllegalArgumentException("Documento requerido (debe generarse mediante regla de transformación)");
        }
        if (fullName == null || fullName.isEmpty()) {
            throw new IllegalArgumentException("Nombre completo requerido");
        }

        // Verificar si el cliente ya existe
        Optional<Customer> existingCustomer = customerRepository.findByTenantIdAndIdentificationCode(tenantId, documento);
        if (existingCustomer.isPresent()) {
            System.out.println("⚠️ Cliente ya existe: " + documento);
            // Por ahora solo retornar el existente sin actualizar
            return null; // No agregar duplicados
        }

        // Crear nuevo cliente
        String codigoIdentificacion = getString(enrichedRowDataObj, "IDENTITY_CODE");
        if (codigoIdentificacion == null) {
            codigoIdentificacion = getString(enrichedRowDataObj, "codigo_identificacion");
        }

        Customer customer = new Customer(
            tenantId,
            documento,  // identificationCode (el documento limpio)
            codigoIdentificacion != null ? codigoIdentificacion : documento,  // customerId (el código original)
            fullName,
            null,
            "ACTIVO"
        );

        System.out.println("✅ Cliente nuevo creado: " + documento + " - " + fullName);
        return customer;
    }

    /**
     * Obtiene un valor String desde el mapa de datos enriquecidos
     */
    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        String strValue = value.toString().trim();
        return strValue.isEmpty() ? null : strValue;
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
