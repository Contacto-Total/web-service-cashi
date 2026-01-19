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
        logger.info("🔍 [checkPeriodStatus] Iniciando para subPortfolioId: {}", subPortfolioId);

        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada: " + subPortfolioId));
        logger.info("✓ SubPortfolio encontrado: {} ({})", subPortfolio.getSubPortfolioName(), subPortfolio.getSubPortfolioCode());

        String tableName = buildTableName(subPortfolio, LoadType.INICIAL);
        logger.info("✓ Nombre de tabla construido: {}", tableName);

        boolean tableExists = dynamicTableExists(tableName);
        logger.info("✓ Tabla existe: {}", tableExists);

        if (!tableExists) {
            logger.info("→ Tabla no existe, retornando PeriodInfo vacío");
            return new PeriodInfo(
                subPortfolioId,
                tableName,
                false,
                0,
                YearMonth.now(),
                null
            );
        }

        // Contar registros y obtener última fecha de carga
        long recordCount = getTableRecordCount(tableName);
        logger.info("✓ RecordCount: {}", recordCount);

        String lastLoadDate = getLastLoadDate(tableName);
        logger.info("✓ LastLoadDate: {}", lastLoadDate);

        YearMonth currentPeriod = determineCurrentPeriod(lastLoadDate);
        logger.info("✓ CurrentPeriod: {}", currentPeriod);

        PeriodInfo result = new PeriodInfo(
            subPortfolioId,
            tableName,
            recordCount > 0,
            recordCount,
            currentPeriod,
            lastLoadDate
        );
        logger.info("✅ [checkPeriodStatus] Completado: {}", result);
        return result;
    }

    @Override
    @Transactional
    public SnapshotResult executeGlobalSnapshot() {
        logger.info("🗄️ Iniciando snapshot global de todas las carteras...");
        long startTime = System.currentTimeMillis();

        try {
            // Ejecutar el stored procedure
            jdbcTemplate.execute("CALL sp_snapshot_carteras_mensual()");

            long executionTime = System.currentTimeMillis() - startTime;
            String archivePeriod = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy_MM"));

            // Contar tablas archivadas (aproximación basada en tablas ini_)
            int tablesArchived = countArchivedTables(archivePeriod);

            logger.info("✅ Snapshot global completado en {}ms. {} tablas archivadas para periodo {}",
                    executionTime, tablesArchived, archivePeriod);

            return new SnapshotResult(
                true,
                tablesArchived,
                archivePeriod,
                "Snapshot global completado exitosamente",
                executionTime
            );

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("❌ Error en snapshot global: {}", e.getMessage(), e);

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

        // SOLO archivamos la tabla INICIAL (ini_), NO la de actualización ni clientes
        String tableIni = buildTableName(subPortfolio, LoadType.INICIAL);
        String archivePeriod = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy_MM"));

        logger.info("🗄️ Iniciando snapshot para subcartera {} - Tabla inicial: {}",
                subPortfolioId, tableIni);
        long startTime = System.currentTimeMillis();

        try {
            // Ejecutar stored procedure (1 solo round-trip, transacción atómica en BD)
            ArchiveResult archiveResult = callArchiveStoredProcedure(tableIni, archivePeriod);

            long executionTime = System.currentTimeMillis() - startTime;

            if (archiveResult.success()) {
                logger.info("✅ Snapshot para subcartera {} completado en {}ms. {} registros archivados. Mensaje: {}",
                        subPortfolioId, executionTime, archiveResult.recordsArchived(), archiveResult.message());

                return new SnapshotResult(
                    true,
                    archiveResult.recordsArchived() > 0 ? 1 : 0,
                    archivePeriod,
                    String.format("Snapshot completado para %s. %d registros archivados.",
                            subPortfolio.getSubPortfolioName(), archiveResult.recordsArchived()),
                    executionTime
                );
            } else {
                logger.error("❌ Stored procedure falló: {}", archiveResult.message());
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
            logger.error("❌ Error en snapshot para subcartera {}: {}", subPortfolioId, e.getMessage(), e);

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

        String tableIni = buildTableName(subPortfolio, LoadType.INICIAL);

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
                    sql, String.class, HISTORIC_DATABASE, tableIni + "_%");

            if (archivedTables.isEmpty()) {
                return Optional.empty();
            }

            // Extraer periodo del nombre de la tabla (ej: ini_sam_mas_elm_2025_01 -> 2025_01)
            String lastTable = archivedTables.get(0);
            String period = lastTable.substring(tableIni.length() + 1); // +1 for the underscore
            return Optional.of(period);

        } catch (Exception e) {
            logger.warn("No se pudo obtener el último periodo archivado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ==================== Métodos privados ====================

    private String buildTableName(SubPortfolio subPortfolio, LoadType loadType) {
        logger.info("  → buildTableName: subPortfolio={}, loadType={}", subPortfolio.getId(), loadType);

        var portfolio = subPortfolio.getPortfolio();
        logger.info("  → portfolio: {}", portfolio != null ? portfolio.getPortfolioCode() : "NULL");

        var tenant = portfolio != null ? portfolio.getTenant() : null;
        logger.info("  → tenant: {}", tenant != null ? tenant.getTenantCode() : "NULL");

        String tenantCode = tenant.getTenantCode().toLowerCase();
        String portfolioCode = portfolio.getPortfolioCode().toLowerCase();
        String subPortfolioCode = subPortfolio.getSubPortfolioCode().toLowerCase();

        String baseName = tenantCode + "_" + portfolioCode + "_" + subPortfolioCode;
        logger.info("  → baseName: {}", baseName);

        if (loadType == LoadType.INICIAL) {
            return "ini_" + baseName;
        }
        return baseName;
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
        logger.info("🔍 [checkDailyStatus] Verificando estado diario para subPortfolioId: {}", subPortfolioId);

        SubPortfolio subPortfolio = subPortfolioRepository.findById(subPortfolioId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Subcartera no encontrada: " + subPortfolioId));

        // Tabla de actualización (sin prefijo ini_)
        String tableName = buildTableName(subPortfolio, LoadType.ACTUALIZACION);
        logger.info("✓ Nombre de tabla actualización: {}", tableName);

        boolean tableExists = dynamicTableExists(tableName);
        logger.info("✓ Tabla existe: {}", tableExists);

        if (!tableExists) {
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

        DailyInfo result = new DailyInfo(
            subPortfolioId,
            tableName,
            recordCount > 0,
            recordCount,
            lastLoadDate,
            lastArchivedDate.orElse(null)
        );

        logger.info("✅ [checkDailyStatus] Completado: {}", result);
        return result;
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

        // Tabla de actualización (sin prefijo ini_)
        String tableActualizacion = buildTableName(subPortfolio, LoadType.ACTUALIZACION);
        String archiveDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy_MM_dd"));

        logger.info("🗄️ Iniciando snapshot diario para subcartera {} - Tabla: {}, Fecha: {}",
                subPortfolioId, tableActualizacion, archiveDate);
        long startTime = System.currentTimeMillis();

        try {
            // Verificar si la tabla existe
            if (!dynamicTableExists(tableActualizacion)) {
                return new SnapshotResult(true, 0, archiveDate,
                    "Tabla no existe, nada que archivar", System.currentTimeMillis() - startTime);
            }

            // Ejecutar stored procedure
            DailyArchiveResult archiveResult = callDailyArchiveStoredProcedure(tableActualizacion, archiveDate);

            long executionTime = System.currentTimeMillis() - startTime;

            if (archiveResult.success()) {
                logger.info("✅ Snapshot diario completado en {}ms. {} registros archivados. Mensaje: {}",
                        executionTime, archiveResult.recordsArchived(), archiveResult.message());

                return new SnapshotResult(
                    true,
                    archiveResult.recordsArchived() > 0 ? 1 : 0,
                    archiveDate,
                    String.format("Snapshot diario completado para %s. %d registros archivados.",
                            subPortfolio.getSubPortfolioName(), archiveResult.recordsArchived()),
                    executionTime
                );
            } else {
                logger.error("❌ Stored procedure diario falló: {}", archiveResult.message());
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
            logger.error("❌ Error en snapshot diario para subcartera {}: {}", subPortfolioId, e.getMessage(), e);

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
