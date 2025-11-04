package com.cashi.customermanagement.application.services;

import com.cashi.customermanagement.domain.model.aggregates.ImportConfiguration;
import com.cashi.customermanagement.domain.model.aggregates.ImportHistory;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ImportConfigurationRepository;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ImportHistoryRepository;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import com.cashi.systemconfiguration.domain.services.HeaderConfigurationCommandService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileWatcherService {

    private static final Logger logger = LoggerFactory.getLogger(FileWatcherService.class);

    private final ImportConfigurationRepository configRepository;
    private final ImportHistoryRepository historyRepository;
    private final HeaderConfigurationCommandService headerConfigService;

    public FileWatcherService(ImportConfigurationRepository configRepository,
                             ImportHistoryRepository historyRepository,
                             HeaderConfigurationCommandService headerConfigService) {
        this.configRepository = configRepository;
        this.historyRepository = historyRepository;
        this.headerConfigService = headerConfigService;
    }

    /**
     * Scheduled task que se ejecuta cada minuto
     * Verifica si es la hora programada para ejecutar la importación automática
     */
    @Scheduled(fixedDelay = 60000) // Cada 1 minuto
    public void checkForNewFiles() {
        try {
            // Buscar configuración activa
            Optional<ImportConfiguration> configOpt = configRepository.findByActiveTrue();

            if (configOpt.isEmpty()) {
                return; // No hay configuración activa
            }

            ImportConfiguration config = configOpt.get();

            // Si hay hora programada, verificar que sea la hora correcta
            if (config.getScheduledTime() != null && !config.getScheduledTime().isEmpty()) {
                String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                String scheduledTime = config.getScheduledTime().substring(0, 5); // "HH:mm:ss" -> "HH:mm"

                // Solo ejecutar si la hora actual coincide con la hora programada
                if (!currentTime.equals(scheduledTime)) {
                    return;
                }

                // Verificar si ya se ejecutó en esta hora (evitar múltiples ejecuciones)
                if (config.getLastCheckAt() != null) {
                    String lastCheckTime = config.getLastCheckAt().format(DateTimeFormatter.ofPattern("HH:mm"));
                    if (lastCheckTime.equals(currentTime)) {
                        logger.debug("Ya se ejecutó la importación en esta hora: {}", currentTime);
                        return;
                    }
                }

                logger.info("⏰ Hora programada alcanzada: {}. Iniciando importación automática.", scheduledTime);
            } else {
                // Modo legacy: usar frecuencia en minutos
                if (config.getLastCheckAt() != null) {
                    LocalDateTime nextCheck;

                    // Si checkFrequencyMinutes es 0, usar 5 segundos (para testing)
                    if (config.getCheckFrequencyMinutes() == 0) {
                        nextCheck = config.getLastCheckAt().plusSeconds(5);
                        logger.debug("Modo testing: revisando cada 5 segundos");
                    } else {
                        nextCheck = config.getLastCheckAt().plusMinutes(config.getCheckFrequencyMinutes());
                    }

                    if (LocalDateTime.now().isBefore(nextCheck)) {
                        return; // Aún no es tiempo de revisar
                    }
                }

                logger.info("Iniciando revisión de archivos. Patrón: {}", config.getFilePattern());
            }

            // Actualizar última revisión
            config.setLastCheckAt(LocalDateTime.now());
            configRepository.save(config);

            // Buscar y procesar archivo
            processNextFile(config);

        } catch (Exception e) {
            logger.error("Error en FileWatcherService: {}", e.getMessage(), e);
        }
    }

    /**
     * Método público para trigger manual de importación
     * Retorna el resultado de la importación incluyendo errores
     */
    public Map<String, Object> triggerManualImport() {
        try {
            // Buscar configuración activa
            Optional<ImportConfiguration> configOpt = configRepository.findByActiveTrue();

            if (configOpt.isEmpty()) {
                return Map.of(
                    "success", false,
                    "message", "No hay configuración de importación activa"
                );
            }

            ImportConfiguration config = configOpt.get();
            logger.info("🔄 Trigger manual de importación iniciado");

            // Buscar archivo a procesar
            File fileToProcess = findNextFileToProcess(config);

            if (fileToProcess == null) {
                return Map.of(
                    "success", false,
                    "message", "No se encontraron archivos para procesar"
                );
            }

            // Procesar archivo y capturar resultado
            return processFileWithResult(fileToProcess, config);

        } catch (Exception e) {
            logger.error("Error en trigger manual: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Error: " + e.getMessage(),
                "errors", List.of(e.getMessage())
            );
        }
    }

    /**
     * Busca el siguiente archivo a procesar
     */
    private File findNextFileToProcess(ImportConfiguration config) {
        try {
            File directory = new File(config.getWatchDirectory());

            if (!directory.exists() || !directory.isDirectory()) {
                logger.warn("El directorio no existe: {}", config.getWatchDirectory());
                return null;
            }

            // Buscar archivos que coincidan con el patrón
            File[] files = directory.listFiles((dir, name) ->
                    name.toLowerCase().contains(config.getFilePattern().toLowerCase()) &&
                            (name.toLowerCase().endsWith(".xlsx") ||
                                    name.toLowerCase().endsWith(".xls") ||
                                    name.toLowerCase().endsWith(".csv"))
            );

            if (files == null || files.length == 0) {
                logger.info("No se encontraron archivos que coincidan con el patrón: {}", config.getFilePattern());
                return null;
            }

            // Ordenar por fecha de modificación (más reciente primero)
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

            // Buscar el primer archivo no procesado
            for (File file : files) {
                boolean alreadyProcessed = historyRepository.existsByFilePathAndStatus(
                        file.getAbsolutePath(),
                        "SUCCESS"
                );

                if (!alreadyProcessed) {
                    return file;
                }
            }

            logger.info("Todos los archivos ya fueron procesados");
            return null;

        } catch (Exception e) {
            logger.error("Error al buscar archivos: {}", e.getMessage(), e);
            return null;
        }
    }

    private void processNextFile(ImportConfiguration config) {
        try {
            File directory = new File(config.getWatchDirectory());

            if (!directory.exists() || !directory.isDirectory()) {
                logger.warn("El directorio no existe: {}", config.getWatchDirectory());
                return;
            }

            // Buscar archivos que coincidan con el patrón
            File[] files = directory.listFiles((dir, name) ->
                    name.toLowerCase().contains(config.getFilePattern().toLowerCase()) &&
                            (name.toLowerCase().endsWith(".xlsx") ||
                                    name.toLowerCase().endsWith(".xls") ||
                                    name.toLowerCase().endsWith(".csv"))
            );

            if (files == null || files.length == 0) {
                logger.info("No se encontraron archivos que coincidan con el patrón: {}", config.getFilePattern());
                return;
            }

            // Ordenar por fecha de modificación (más reciente primero)
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

            // Buscar el primer archivo no procesado
            File fileToProcess = null;
            for (File file : files) {
                boolean alreadyProcessed = historyRepository.existsByFilePathAndStatus(
                        file.getAbsolutePath(),
                        "SUCCESS"
                );

                if (!alreadyProcessed) {
                    fileToProcess = file;
                    break;
                }
            }

            if (fileToProcess == null) {
                logger.info("Todos los archivos ya fueron procesados");
                return;
            }

            logger.info("Procesando archivo: {}", fileToProcess.getName());

            // Procesar el archivo
            processFile(fileToProcess, config);

        } catch (Exception e) {
            logger.error("Error al procesar archivos: {}", e.getMessage(), e);
        }
    }

    /**
     * Procesa un archivo y retorna el resultado (usado para trigger manual)
     */
    private Map<String, Object> processFileWithResult(File file, ImportConfiguration config) {
        String filePath = file.getAbsolutePath();
        String fileName = file.getName();

        try {
            logger.info("Importando datos del archivo: {}", fileName);

            // Obtener subPortfolioId de la configuración
            Integer subPortfolioId = config.getSubPortfolioId();

            if (subPortfolioId == null) {
                throw new IllegalArgumentException("No se ha configurado la subcartera para la importación automática");
            }

            logger.info("Importando a subcartera ID: {}", subPortfolioId);

            // Leer el archivo y convertir a List<Map<String, Object>>
            List<Map<String, Object>> data = readFileData(file);

            if (data.isEmpty()) {
                logger.warn("El archivo {} no contiene datos para importar", fileName);
                return Map.of(
                    "success", false,
                    "message", "El archivo no contiene datos para importar",
                    "fileName", fileName
                );
            }

            logger.info("Datos leídos: {} filas", data.size());

            // Importar a tabla dinámica usando HeaderConfigurationCommandService
            // SOLO usa LoadType.ACTUALIZACION (tablas act_*)
            Map<String, Object> result = headerConfigService.importDataToTable(
                    subPortfolioId,
                    LoadType.ACTUALIZACION,
                    data
            );

            // Extraer información del resultado
            Integer insertedRows = (Integer) result.get("insertedRows");
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) result.get("errors");

            int recordsProcessed = insertedRows != null ? insertedRows : 0;

            // Construir mensaje de error si hubo errores
            String errorMessage = null;
            if (errors != null && !errors.isEmpty()) {
                errorMessage = "Se encontraron " + errors.size() + " errores durante la importación";
                logger.warn("Errores en importación: {}", errors);
            }

            // Registrar en historial
            String status = (errors == null || errors.isEmpty()) ? "SUCCESS" : "SUCCESS_WITH_ERRORS";
            ImportHistory history = new ImportHistory(
                    subPortfolioId.longValue(),
                    fileName,
                    filePath,
                    status,
                    recordsProcessed,
                    errorMessage
            );
            historyRepository.save(history);

            logger.info("Archivo procesado: {} ({} registros, {} errores)",
                    fileName, recordsProcessed, errors != null ? errors.size() : 0);

            // Mover archivo a carpeta de procesados si está configurado
            if (config.getMoveAfterProcess() && config.getProcessedDirectory() != null) {
                moveFileToProcessed(file, config.getProcessedDirectory());
            }

            // Retornar resultado completo
            return Map.of(
                "success", true,
                "message", "Archivo procesado exitosamente",
                "fileName", fileName,
                "insertedRows", recordsProcessed,
                "errors", errors != null ? errors : List.of(),
                "hasErrors", errors != null && !errors.isEmpty()
            );

        } catch (Exception e) {
            logger.error("Error al procesar archivo {}: {}", fileName, e.getMessage(), e);

            // Registrar en historial como error
            ImportHistory history = new ImportHistory(
                    config.getSubPortfolioId() != null ? config.getSubPortfolioId().longValue() : null,
                    fileName,
                    filePath,
                    "ERROR",
                    0,
                    e.getMessage()
            );

            try {
                historyRepository.save(history);
            } catch (Exception saveError) {
                logger.error("Error al guardar historial de error: {}", saveError.getMessage());
            }

            // Mover archivo a carpeta de errores si está configurado
            if (config.getMoveAfterProcess() && config.getErrorDirectory() != null) {
                try {
                    moveFileToError(file, config.getErrorDirectory());
                } catch (IOException moveException) {
                    logger.error("Error al mover archivo a carpeta de errores: {}", moveException.getMessage());
                }
            }

            // Retornar error
            return Map.of(
                "success", false,
                "message", "Error al procesar archivo: " + e.getMessage(),
                "fileName", fileName,
                "errors", List.of(e.getMessage())
            );
        }
    }

    /**
     * Procesa un archivo (versión legacy para scheduled task)
     */
    private void processFile(File file, ImportConfiguration config) {
        processFileWithResult(file, config);
    }

    /**
     * Lee un archivo Excel o CSV y retorna los datos como List<Map<String, Object>>
     */
    private List<Map<String, Object>> readFileData(File file) throws IOException {
        String fileName = file.getName().toLowerCase();

        // Intentar leer como Excel primero si tiene extensión .xlsx o .xls
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            try {
                return readExcelData(file, fileName.endsWith(".xlsx"));
            } catch (Exception e) {
                // Si falla, intentar como CSV
                logger.warn("No se pudo leer como Excel, intentando como CSV: {}", e.getMessage());
                return readCsvData(file);
            }
        } else if (fileName.endsWith(".csv")) {
            return readCsvData(file);
        } else {
            throw new IllegalArgumentException("Formato de archivo no soportado: " + fileName);
        }
    }

    /**
     * Lee datos de archivo Excel
     */
    private List<Map<String, Object>> readExcelData(File file, boolean isXlsx) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = isXlsx ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException("El archivo no tiene fila de cabeceras");
            }

            // Extraer nombres de columnas
            List<String> columnNames = new ArrayList<>();
            for (Cell cell : headerRow) {
                String headerName = getCellValueAsString(cell).trim();
                columnNames.add(headerName);
            }

            logger.info("📋 Cabeceras encontradas en Excel: {}", columnNames);

            // Leer filas de datos
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < columnNames.size(); j++) {
                    Cell cell = row.getCell(j);
                    String columnName = columnNames.get(j);
                    Object value = getCellValueAsString(cell);
                    rowData.put(columnName, value);
                }

                data.add(rowData);
            }
        }

        return data;
    }

    /**
     * Lee datos de archivo CSV con auto-detección de separador
     */
    private List<Map<String, Object>> readCsvData(File file) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();

        // Leer todo el contenido para detectar el separador
        byte[] allBytes = Files.readAllBytes(file.toPath());
        String content = new String(allBytes);

        // Auto-detectar separador
        String firstLine = content.lines().findFirst().orElse("");
        char delimiter = ',';
        if (firstLine.contains(";") && !firstLine.contains(",")) {
            delimiter = ';';
        } else if (firstLine.contains(";") && firstLine.contains(",")) {
            long commas = firstLine.chars().filter(ch -> ch == ',').count();
            long semicolons = firstLine.chars().filter(ch -> ch == ';').count();
            delimiter = semicolons > commas ? ';' : ',';
        }

        logger.info("CSV detectado con separador: '{}'", delimiter);

        // Parsear CSV
        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(new StringReader(content))) {

            // Log de cabeceras detectadas
            if (parser.getHeaderMap() != null) {
                logger.info("📋 Cabeceras encontradas en CSV: {}", parser.getHeaderNames());
            }

            int rowNum = 0;
            for (CSVRecord record : parser) {
                Map<String, Object> rowData = new HashMap<>();
                record.toMap().forEach(rowData::put);
                data.add(rowData);

                // Log solo de la primera fila para debug
                if (rowNum == 0) {
                    logger.info("📊 Primera fila de datos (muestra): {}",
                        rowData.entrySet().stream()
                            .limit(5)
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(java.util.stream.Collectors.joining(", ")));
                }
                rowNum++;
            }

            logger.info("✅ Total filas leídas del CSV: {}", data.size());
        }

        return data;
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

    private void moveFileToProcessed(File file, String processedDirectory) throws IOException {
        // Crear subcarpeta con la fecha actual
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path targetDir = Paths.get(processedDirectory, dateFolder);

        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(file.getName());
        Files.move(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("Archivo movido a: {}", targetPath);
    }

    private void moveFileToError(File file, String errorDirectory) throws IOException {
        // Crear subcarpeta con la fecha actual
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path targetDir = Paths.get(errorDirectory, dateFolder);

        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(file.getName());
        Files.move(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("Archivo con error movido a: {}", targetPath);
    }
}
