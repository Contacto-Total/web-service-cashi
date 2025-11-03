package com.cashi.customermanagement.application.services;

import com.cashi.customermanagement.domain.model.aggregates.ImportConfiguration;
import com.cashi.customermanagement.domain.model.aggregates.ImportHistory;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ImportConfigurationRepository;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ImportHistoryRepository;
import com.cashi.customermanagement.interfaces.rest.resources.*;
import com.cashi.shared.domain.model.entities.HeaderConfiguration;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import com.cashi.systemconfiguration.domain.services.HeaderConfigurationQueryService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImportConfigService {

    private final ImportConfigurationRepository configRepository;
    private final ImportHistoryRepository historyRepository;
    private final HeaderConfigurationQueryService headerConfigService;

    public ImportConfigService(ImportConfigurationRepository configRepository,
                              ImportHistoryRepository historyRepository,
                              HeaderConfigurationQueryService headerConfigService) {
        this.configRepository = configRepository;
        this.historyRepository = historyRepository;
        this.headerConfigService = headerConfigService;
    }

    @Transactional
    public ImportConfigResource saveConfig(ImportConfigRequest request) {
        // Verificar si ya existe una configuración
        ImportConfiguration config = configRepository.findFirstByOrderByIdDesc()
                .orElse(new ImportConfiguration());

        config.setWatchDirectory(request.watchDirectory());
        config.setFilePattern(request.filePattern());
        config.setSubPortfolioId(request.subPortfolioId());
        config.setCheckFrequencyMinutes(request.checkFrequencyMinutes());
        config.setActive(request.active());
        config.setProcessedDirectory(request.processedDirectory());
        config.setErrorDirectory(request.errorDirectory());
        config.setMoveAfterProcess(request.moveAfterProcess());

        config = configRepository.save(config);

        return toResource(config);
    }

    public ImportConfigResource getConfig() {
        ImportConfiguration config = configRepository.findFirstByOrderByIdDesc()
                .orElse(null);

        if (config == null) {
            // Retornar configuración por defecto
            return new ImportConfigResource(
                    null,
                    "",
                    "",
                    null,
                    15,
                    false,
                    "",
                    "",
                    true,
                    null
            );
        }

        return toResource(config);
    }

    public List<ImportHistoryResource> getHistory(Long subPortfolioId) {
        if (subPortfolioId != null) {
            return historyRepository.findBySubPortfolioIdOrderByProcessedAtDesc(subPortfolioId)
                    .stream()
                    .map(this::toHistoryResource)
                    .collect(Collectors.toList());
        } else {
            // Últimos 50 registros
            return historyRepository.findAll()
                    .stream()
                    .sorted(Comparator.comparing(ImportHistory::getProcessedAt).reversed())
                    .limit(50)
                    .map(this::toHistoryResource)
                    .collect(Collectors.toList());
        }
    }

    public List<FilePreviewResource> scanFolder(String watchDirectory, String filePattern) {
        List<FilePreviewResource> files = new ArrayList<>();

        try {
            File directory = new File(watchDirectory);

            if (!directory.exists() || !directory.isDirectory()) {
                throw new RuntimeException("El directorio no existe o no es válido: " + watchDirectory);
            }

            File[] foundFiles = directory.listFiles((dir, name) ->
                    name.toLowerCase().contains(filePattern.toLowerCase()) &&
                            (name.toLowerCase().endsWith(".xlsx") ||
                                    name.toLowerCase().endsWith(".xls") ||
                                    name.toLowerCase().endsWith(".csv"))
            );

            if (foundFiles != null) {
                for (File file : foundFiles) {
                    try {
                        Path filePath = file.toPath();
                        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

                        LocalDateTime modifiedDate = LocalDateTime.ofInstant(
                                attrs.lastModifiedTime().toInstant(),
                                ZoneId.systemDefault()
                        );

                        // Verificar si ya fue procesado
                        boolean processed = historyRepository.existsByFilePathAndStatus(
                                file.getAbsolutePath(),
                                "SUCCESS"
                        );

                        files.add(new FilePreviewResource(
                                file.getName(),
                                formatFileSize(file.length()),
                                modifiedDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                processed
                        ));
                    } catch (Exception e) {
                        System.err.println("Error leyendo archivo: " + file.getName() + " - " + e.getMessage());
                    }
                }

                // Ordenar por fecha de modificación (más reciente primero)
                files.sort((f1, f2) -> f2.modifiedDate().compareTo(f1.modifiedDate()));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al escanear la carpeta: " + e.getMessage(), e);
        }

        return files;
    }

    private ImportConfigResource toResource(ImportConfiguration config) {
        return new ImportConfigResource(
                config.getId(),
                config.getWatchDirectory(),
                config.getFilePattern(),
                config.getSubPortfolioId(),
                config.getCheckFrequencyMinutes(),
                config.getActive(),
                config.getProcessedDirectory(),
                config.getErrorDirectory(),
                config.getMoveAfterProcess(),
                config.getLastCheckAt() != null ?
                        config.getLastCheckAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null
        );
    }

    private ImportHistoryResource toHistoryResource(ImportHistory history) {
        return new ImportHistoryResource(
                history.getId(),
                history.getFileName(),
                history.getFilePath(),
                history.getProcessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                history.getStatus(),
                history.getRecordsProcessed(),
                history.getErrorMessage()
        );
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Validates that file headers match the configured headers for a subportfolio
     */
    public HeaderValidationResource validateFileHeaders(String filePath, Integer subPortfolioId, LoadType loadType) {
        try {
            // 1. Read file headers
            List<String> fileHeaders = readFileHeaders(filePath);

            if (fileHeaders.isEmpty()) {
                return new HeaderValidationResource(
                    false,
                    "El archivo no contiene cabeceras o está vacío"
                );
            }

            // 2. Get configured headers for the subportfolio
            List<HeaderConfiguration> configuredHeaders = headerConfigService
                .getAllBySubPortfolioAndLoadType(subPortfolioId, loadType);

            if (configuredHeaders.isEmpty()) {
                return new HeaderValidationResource(
                    false,
                    "No hay cabeceras configuradas para esta subcartera y tipo de carga"
                );
            }

            // 3. Extract configured header names (case insensitive)
            Set<String> configuredHeaderNames = configuredHeaders.stream()
                .map(h -> h.getHeaderName().toLowerCase().trim())
                .collect(Collectors.toSet());

            // 4. Normalize file headers
            Set<String> normalizedFileHeaders = fileHeaders.stream()
                .map(h -> h.toLowerCase().trim())
                .collect(Collectors.toSet());

            // 5. Find missing required headers
            List<String> missingHeaders = configuredHeaders.stream()
                .filter(h -> h.getRequired() == 1)
                .map(HeaderConfiguration::getHeaderName)
                .filter(name -> !normalizedFileHeaders.contains(name.toLowerCase().trim()))
                .collect(Collectors.toList());

            if (!missingHeaders.isEmpty()) {
                return new HeaderValidationResource(
                    false,
                    "Faltan cabeceras obligatorias: " + String.join(", ", missingHeaders)
                );
            }

            // 6. Find unexpected headers in file
            List<String> unexpectedHeaders = fileHeaders.stream()
                .filter(fh -> !configuredHeaderNames.contains(fh.toLowerCase().trim()))
                .collect(Collectors.toList());

            String message = "Validación exitosa. ";
            if (!unexpectedHeaders.isEmpty()) {
                message += "Advertencia: El archivo contiene cabeceras adicionales no configuradas: "
                    + String.join(", ", unexpectedHeaders);
            } else {
                message += "Todas las cabeceras coinciden perfectamente.";
            }

            return new HeaderValidationResource(true, message);

        } catch (Exception e) {
            return new HeaderValidationResource(
                false,
                "Error al validar cabeceras: " + e.getMessage()
            );
        }
    }

    /**
     * Reads headers from an Excel or CSV file
     * Tries to auto-detect the real format if extension is misleading
     */
    private List<String> readFileHeaders(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new FileNotFoundException("Archivo no encontrado: " + filePath);
        }

        String fileName = file.getName().toLowerCase();

        // Try to read based on extension, with fallback to CSV if it fails
        if (fileName.endsWith(".xlsx")) {
            try {
                return readExcelHeaders(file, true);
            } catch (Exception e) {
                // If fails, try as CSV (some .xlsx files are actually CSV)
                System.out.println("⚠️ Archivo .xlsx no es Excel válido, intentando como CSV...");
                return readCsvHeaders(file);
            }
        } else if (fileName.endsWith(".xls")) {
            try {
                return readExcelHeaders(file, false);
            } catch (Exception e) {
                // If fails, try as CSV (many .xls files are actually CSV in practice)
                System.out.println("⚠️ Archivo .xls no es Excel válido, intentando como CSV...");
                return readCsvHeaders(file);
            }
        } else if (fileName.endsWith(".csv")) {
            return readCsvHeaders(file);
        } else {
            throw new IllegalArgumentException("Formato de archivo no soportado: " + fileName);
        }
    }

    /**
     * Reads headers from Excel file (.xlsx or .xls)
     */
    private List<String> readExcelHeaders(File file, boolean isXlsx) throws IOException {
        List<String> headers = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = isXlsx ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                return headers;
            }

            for (Cell cell : headerRow) {
                String headerValue = getCellValueAsString(cell);
                if (headerValue != null && !headerValue.trim().isEmpty()) {
                    headers.add(headerValue.trim());
                }
            }
        }

        return headers;
    }

    /**
     * Reads headers from CSV file
     * Auto-detects separator (comma or semicolon)
     */
    private List<String> readCsvHeaders(File file) throws IOException {
        List<String> headers = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String firstLine = reader.readLine();

            if (firstLine != null && !firstLine.trim().isEmpty()) {
                // Auto-detect separator: comma or semicolon
                String separator = ",";
                if (firstLine.contains(";") && !firstLine.contains(",")) {
                    separator = ";";
                } else if (firstLine.contains(";") && firstLine.contains(",")) {
                    // Count which appears more frequently
                    long commas = firstLine.chars().filter(ch -> ch == ',').count();
                    long semicolons = firstLine.chars().filter(ch -> ch == ';').count();
                    separator = semicolons > commas ? ";" : ",";
                }

                // Split by detected separator, handling quoted values
                String regex = separator + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
                String[] headerArray = firstLine.split(regex);

                for (String header : headerArray) {
                    // Remove quotes and trim
                    String cleaned = header.replaceAll("^\"|\"$", "").trim();
                    if (!cleaned.isEmpty()) {
                        headers.add(cleaned);
                    }
                }

                System.out.println("📄 CSV detectado con separador: '" + separator + "', " + headers.size() + " cabeceras encontradas");
            }
        }

        return headers;
    }

    /**
     * Extracts cell value as string regardless of cell type
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}
