package com.cashi.systemconfiguration.application.internal.commandservices;

import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.valueobjects.LoadType;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
import com.cashi.systemconfiguration.domain.services.PeriodSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PeriodSnapshotServiceImpl implements PeriodSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(PeriodSnapshotServiceImpl.class);
    private static final String HISTORIC_DATABASE = "cashi_historico_db";
    private static final String MAIN_DATABASE = "cashi_db";

    private final JdbcTemplate jdbcTemplate;
    private final SubPortfolioRepository subPortfolioRepository;

    public PeriodSnapshotServiceImpl(JdbcTemplate jdbcTemplate, SubPortfolioRepository subPortfolioRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.subPortfolioRepository = subPortfolioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PeriodInfo checkPeriodStatus(Long subPortfolioId) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada: " + subPortfolioId));

        String tableName = buildTableName(subPortfolio, LoadType.INICIAL);
        boolean tableExists = dynamicTableExists(tableName);

        if (!tableExists) {
            logger.debug("Tabla {} no existe para subcartera {}", tableName, subPortfolioId);
            return new PeriodInfo(
                subPortfolioId,
                tableName,
                false,
                0,
                YearMonth.now(),
                null
            );
        }

        long recordCount = getTableRecordCount(tableName);
        String lastLoadDate = getLastLoadDate(tableName);
        YearMonth currentPeriod = determineCurrentPeriod(lastLoadDate);

        return new PeriodInfo(
            subPortfolioId,
            tableName,
            recordCount > 0,
            recordCount,
            currentPeriod,
            lastLoadDate
        );
    }

    @Override
    @Transactional
    public SnapshotResult executeGlobalSnapshot() {
        logger.info("Iniciando snapshot global de todas las carteras");
        long startTime = System.currentTimeMillis();

        try {
            jdbcTemplate.execute("CALL sp_snapshot_carteras_mensual()");

            long executionTime = System.currentTimeMillis() - startTime;
            String archivePeriod = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy_MM"));
            int tablesArchived = countArchivedTables(archivePeriod);

            logger.info("Snapshot global completado: {} tablas archivadas, periodo {}, {}ms",
                    tablesArchived, archivePeriod, executionTime);

            return new SnapshotResult(
                true,
                tablesArchived,
                archivePeriod,
                "Snapshot global completado exitosamente",
                executionTime
            );

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Error en snapshot global: {}", e.getMessage(), e);

            return new SnapshotResult(
                false,
                0,
                null,
                "Error: " + e.getMessage(),
                executionTime
            );
        }
    }

    @Override
    @Transactional
    public SnapshotResult executeSnapshotForSubPortfolio(Long subPortfolioId) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada: " + subPortfolioId));

        String tableInicial = buildTableName(subPortfolio, LoadType.INICIAL);
        String archivePeriod = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy_MM"));
        long startTime = System.currentTimeMillis();

        try {
            ArchiveResult archiveResult = callArchiveStoredProcedure(tableInicial, archivePeriod);
            long executionTime = System.currentTimeMillis() - startTime;

            if (archiveResult.success()) {
                logger.info("Snapshot subcartera {}: {} registros archivados, {}ms",
                        subPortfolioId, archiveResult.recordsArchived(), executionTime);

                return new SnapshotResult(
                    true,
                    archiveResult.recordsArchived() > 0 ? 1 : 0,
                    archivePeriod,
                    String.format("Snapshot completado para %s. %d registros archivados.",
                            subPortfolio.getSubPortfolioName(), archiveResult.recordsArchived()),
                    executionTime
                );
            } else {
                logger.error("Snapshot fallido para subcartera {}: {}", subPortfolioId, archiveResult.message());
                return new SnapshotResult(
                    false,
                    0,
                    archivePeriod,
                    archiveResult.message(),
                    executionTime
                );
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Error en snapshot subcartera {}: {}", subPortfolioId, e.getMessage(), e);

            return new SnapshotResult(
                false,
                0,
                archivePeriod,
                "Error: " + e.getMessage(),
                executionTime
            );
        }
    }

    /**
     * Record interno para el resultado del stored procedure
     */
    private record ArchiveResult(boolean success, long recordsArchived, String message) {}

    /**
     * Llama al stored procedure sp_archivar_periodo
     * Ejecuta todo en 1 round-trip con transacción atómica en la BD
     */
    private ArchiveResult callArchiveStoredProcedure(String tableName, String archivePeriod) {
        return jdbcTemplate.execute(connection -> {
            CallableStatement cs = connection.prepareCall("{CALL sp_archivar_periodo(?, ?, ?, ?, ?)}");
            cs.setString(1, tableName);
            cs.setString(2, archivePeriod);
            cs.registerOutParameter(3, Types.BIGINT);   // p_records_archived
            cs.registerOutParameter(4, Types.BOOLEAN);  // p_success
            cs.registerOutParameter(5, Types.VARCHAR);  // p_message
            return cs;
        }, (CallableStatement cs) -> {
            cs.execute();
            long recordsArchived = cs.getLong(3);
            boolean success = cs.getBoolean(4);
            String message = cs.getString(5);
            return new ArchiveResult(success, recordsArchived, message);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean requiresPeriodChangeConfirmation(Long subPortfolioId) {
        PeriodInfo periodInfo = checkPeriodStatus(subPortfolioId);
        return periodInfo.hasExistingData();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getLastArchivedPeriod(Long subPortfolioId) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada: " + subPortfolioId));

        String tableInicial = buildTableName(subPortfolio, LoadType.INICIAL);

        try {
            // Buscar tablas archivadas en la BD histórica
            String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                AND table_name LIKE ?
                ORDER BY table_name DESC
                LIMIT 1
                """;

            List<String> archivedTables = jdbcTemplate.queryForList(
                    sql, String.class, HISTORIC_DATABASE, tableInicial + "_%");

            if (archivedTables.isEmpty()) {
                return Optional.empty();
            }

            // Extraer periodo del nombre de la tabla (ej: sam_mas_elm_2025_01 -> 2025_01)
            String lastTable = archivedTables.get(0);
            String period = lastTable.substring(tableInicial.length() + 1); // +1 for the underscore
            return Optional.of(period);

        } catch (Exception e) {
            logger.warn("No se pudo obtener el último periodo archivado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ==================== Métodos privados ====================

    private String buildTableName(SubPortfolio subPortfolio, LoadType loadType) {
        var portfolio = subPortfolio.getPortfolio();
        var tenant = portfolio.getTenant();

        String tenantCode = tenant.getTenantCode().toLowerCase();
        String portfolioCode = portfolio.getPortfolioCode().toLowerCase();
        String subPortfolioCode = subPortfolio.getSubPortfolioCode().toLowerCase();

        String baseName = tenantCode + "_" + portfolioCode + "_" + subPortfolioCode;
        return loadType.getTablePrefix() + baseName;
    }

    private boolean dynamicTableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, MAIN_DATABASE, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private long getTableRecordCount(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM " + tableName;
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getLastLoadDate(String tableName) {
        try {
            // Intentar obtener la fecha más reciente de la columna de carga o actualización
            String sql = "SELECT MAX(COALESCE(fecha_carga, fecha_actualizacion, created_at)) FROM " + tableName;
            Object result = jdbcTemplate.queryForObject(sql, Object.class);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            // Si falla, intentar con la fecha de modificación de la tabla
            try {
                String sql = """
                    SELECT UPDATE_TIME FROM information_schema.tables
                    WHERE table_schema = ? AND table_name = ?
                    """;
                Object result = jdbcTemplate.queryForObject(sql, Object.class, MAIN_DATABASE, tableName);
                return result != null ? result.toString() : null;
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private YearMonth determineCurrentPeriod(String lastLoadDate) {
        if (lastLoadDate == null) {
            return YearMonth.now();
        }
        try {
            // Intentar parsear la fecha para obtener año-mes
            LocalDate date = LocalDate.parse(lastLoadDate.substring(0, 10));
            return YearMonth.from(date);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    // NOTA: El método archiveTable fue reemplazado por el stored procedure sp_archivar_periodo
    // que ejecuta toda la lógica en 1 round-trip con transacción atómica en la BD

    private int countArchivedTables(String archivePeriod) {
        try {
            String sql = """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = ? AND table_name LIKE ?
                """;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                    HISTORIC_DATABASE, "%_" + archivePeriod);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // NOTA: insertNotification ya no se usa aquí, el stored procedure lo hace internamente

    // ==================== Métodos para Snapshot Diario ====================

    @Override
    @Transactional(readOnly = true)
    public DailyInfo checkDailyStatus(Long subPortfolioId) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada: " + subPortfolioId));

        String tableName = buildTableName(subPortfolio, LoadType.ACTUALIZACION);
        boolean tableExists = dynamicTableExists(tableName);

        if (!tableExists) {
            logger.debug("Tabla diaria {} no existe para subcartera {}", tableName, subPortfolioId);
            return new DailyInfo(
                subPortfolioId,
                tableName,
                false,
                0,
                null,
                null
            );
        }

        long recordCount = getTableRecordCount(tableName);
        String lastLoadDate = getLastLoadDate(tableName);
        Optional<String> lastArchivedDate = getLastArchivedDailyDate(subPortfolioId);

        return new DailyInfo(
            subPortfolioId,
            tableName,
            recordCount > 0,
            recordCount,
            lastLoadDate,
            lastArchivedDate.orElse(null)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean requiresDailyChangeConfirmation(Long subPortfolioId) {
        DailyInfo dailyInfo = checkDailyStatus(subPortfolioId);
        return dailyInfo.hasExistingData();
    }

    @Override
    @Transactional
    public SnapshotResult executeDailySnapshotForSubPortfolio(Long subPortfolioId) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada: " + subPortfolioId));

        String tableActualizacion = buildTableName(subPortfolio, LoadType.ACTUALIZACION);
        String archiveDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        long startTime = System.currentTimeMillis();

        try {
            if (!dynamicTableExists(tableActualizacion)) {
                return new SnapshotResult(true, 0, archiveDate,
                    "Tabla no existe, nada que archivar", System.currentTimeMillis() - startTime);
            }

            DailyArchiveResult archiveResult = callDailyArchiveStoredProcedure(tableActualizacion, archiveDate);
            long executionTime = System.currentTimeMillis() - startTime;

            if (archiveResult.success()) {
                logger.info("Snapshot diario subcartera {}: {} registros archivados, {}ms",
                        subPortfolioId, archiveResult.recordsArchived(), executionTime);

                return new SnapshotResult(
                    true,
                    archiveResult.recordsArchived() > 0 ? 1 : 0,
                    archiveDate,
                    String.format("Snapshot diario completado para %s. %d registros archivados.",
                            subPortfolio.getSubPortfolioName(), archiveResult.recordsArchived()),
                    executionTime
                );
            } else {
                logger.error("Snapshot diario fallido para subcartera {}: {}", subPortfolioId, archiveResult.message());
                return new SnapshotResult(
                    false,
                    0,
                    archiveDate,
                    archiveResult.message(),
                    executionTime
                );
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Error en snapshot diario subcartera {}: {}", subPortfolioId, e.getMessage(), e);

            return new SnapshotResult(
                false,
                0,
                archiveDate,
                "Error: " + e.getMessage(),
                executionTime
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getLastArchivedDailyDate(Long subPortfolioId) {
        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada: " + subPortfolioId));

        String tableActualizacion = buildTableName(subPortfolio, LoadType.ACTUALIZACION);

        try {
            // Buscar tablas archivadas diarias en la BD histórica
            // Formato: sam_mas_elm_YYYY_MM_DD
            String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                AND table_name LIKE ?
                AND table_name REGEXP ?
                ORDER BY table_name DESC
                LIMIT 1
                """;

            // Regex para capturar fecha YYYY_MM_DD al final
            String pattern = tableActualizacion + "_[0-9]{4}_[0-9]{2}_[0-9]{2}$";

            List<String> archivedTables = jdbcTemplate.queryForList(
                    sql, String.class, HISTORIC_DATABASE, tableActualizacion + "_%", pattern);

            if (archivedTables.isEmpty()) {
                return Optional.empty();
            }

            // Extraer fecha del nombre de la tabla (ej: sam_mas_elm_2025_01_19 -> 2025_01_19)
            String lastTable = archivedTables.get(0);
            String date = lastTable.substring(tableActualizacion.length() + 1); // +1 for underscore
            return Optional.of(date);

        } catch (Exception e) {
            logger.warn("No se pudo obtener la última fecha archivada diaria: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Record interno para el resultado del stored procedure diario
     */
    private record DailyArchiveResult(boolean success, long recordsArchived, String message) {}

    /**
     * Llama al stored procedure sp_archivar_diario
     */
    private DailyArchiveResult callDailyArchiveStoredProcedure(String tableName, String archiveDate) {
        return jdbcTemplate.execute(connection -> {
            CallableStatement cs = connection.prepareCall("{CALL sp_archivar_diario(?, ?, ?, ?, ?)}");
            cs.setString(1, tableName);
            cs.setString(2, archiveDate);
            cs.registerOutParameter(3, Types.BIGINT);   // p_records_archived
            cs.registerOutParameter(4, Types.BOOLEAN);  // p_success
            cs.registerOutParameter(5, Types.VARCHAR);  // p_message
            return cs;
        }, (CallableStatement cs) -> {
            cs.execute();
            long recordsArchived = cs.getLong(3);
            boolean success = cs.getBoolean(4);
            String message = cs.getString(5);
            return new DailyArchiveResult(success, recordsArchived, message);
        });
    }
}
