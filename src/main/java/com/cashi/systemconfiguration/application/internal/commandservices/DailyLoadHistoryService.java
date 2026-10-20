package com.cashi.systemconfiguration.application.internal.commandservices;

import com.cashi.customermanagement.domain.model.aggregates.ImportHistory;
import com.cashi.customermanagement.infrastructure.persistence.jpa.repositories.ImportHistoryRepository;
import com.cashi.shared.util.DailyFileDateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Historial de los archivos de carga diaria manual en historial_importaciones.
 * La fecha del último archivo cargado (tomada de su nombre) nombra la tabla histórica
 * cuando el snapshot diario archiva ini_<tabla> antes de la carga siguiente.
 */
@Service
public class DailyLoadHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(DailyLoadHistoryService.class);

    public static final String RUTA_CARGA_DIARIA_MANUAL = "CARGA_DIARIA_MANUAL";
    private static final String ESTADO_OK = "SUCCESS";
    private static final String ESTADO_ERROR = "ERROR";
    private static final String SIN_NOMBRE = "(sin nombre)";

    private final ImportHistoryRepository importHistoryRepository;

    public DailyLoadHistoryService(ImportHistoryRepository importHistoryRepository) {
        this.importHistoryRepository = importHistoryRepository;
    }

    public void recordSuccess(Integer subPortfolioId, String fileName, Integer recordsProcessed) {
        record(subPortfolioId, fileName, ESTADO_OK, recordsProcessed, null);
    }

    public void recordError(Integer subPortfolioId, String fileName, String errorMessage) {
        record(subPortfolioId, fileName, ESTADO_ERROR, null, errorMessage);
    }

    /**
     * Fecha (yyyy_MM_dd) del último archivo cargado con éxito, o sea de lo que hay hoy en ini_<tabla>.
     * Vacío si no hay cargas registradas o si el nombre guardado no trae una fecha válida.
     */
    public Optional<String> findLastLoadedFileDate(Integer subPortfolioId) {
        return importHistoryRepository
                .findFirstBySubPortfolioIdAndFilePathAndStatusOrderByProcessedAtDescIdDesc(
                        subPortfolioId.longValue(), RUTA_CARGA_DIARIA_MANUAL, ESTADO_OK)
                .flatMap(h -> DailyFileDateUtil.extractArchiveDate(h.getFileName()));
    }

    /**
     * Registra la carga. Si el INSERT falla, la carga no falla: solo queda un warning en el log.
     * Corre fuera de la transacción de la carga (save abre la suya), así un error aquí no la revierte.
     */
    private void record(Integer subPortfolioId, String fileName, String status, Integer recordsProcessed,
                        String errorMessage) {
        try {
            String nombre = fileName == null || fileName.isBlank() ? SIN_NOMBRE : fileName;
            importHistoryRepository.save(new ImportHistory(subPortfolioId.longValue(), nombre,
                    RUTA_CARGA_DIARIA_MANUAL, status, recordsProcessed, errorMessage));
        } catch (Exception e) {
            logger.warn("No se pudo registrar la carga diaria de la subcartera {} en historial_importaciones: {}",
                    subPortfolioId, e.getMessage());
        }
    }
}
