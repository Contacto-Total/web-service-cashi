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
            int tablesArchived = 0;

            // SOLO archivar tabla INICIAL (ini_) si existe
            if (dynamicTableExists(tableIni)) {
                archiveTable(tableIni, archivePeriod);
                tablesArchived++;
                logger.info("✓ Tabla inicial {} archivada y limpiada", tableIni);
            } else {
                logger.warn("⚠️ Tabla inicial {} no existe, nada que archivar", tableIni);
            }

            // NOTA: NO archivamos la tabla de actualización ni la tabla clientes
            // Solo se archiva la tabla ini_ que contiene la carga inicial del mes

            long executionTime = System.currentTimeMillis() - startTime;

            // Registrar en notificaciones
            insertNotification("ARCHIVADO_SUBCARTERA",
                    "Snapshot subcartera " + subPortfolio.getSubPortfolioCode(),
                    String.format("Tabla inicial archivada para periodo %s", archivePeriod));

            logger.info("✅ Snapshot para subcartera {} completado en {}ms. {} tabla(s) archivada(s)",
                    subPortfolioId, executionTime, tablesArchived);

            return new SnapshotResult(
                true,
                tablesArchived,
                archivePeriod,
                String.format("Snapshot completado para %s. Tabla inicial archivada.",
                        subPortfolio.getSubPortfolioName()),
                executionTime
            );

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

    private void archiveTable(String tableName, String archivePeriod) {
        String archivedTableName = tableName + "_" + archivePeriod;

        logger.info("📦 Archivando tabla {} -> {}.{}", tableName, HISTORIC_DATABASE, archivedTableName);

        // Asegurar que la base de datos histórica existe
        String createDbSql = String.format("CREATE DATABASE IF NOT EXISTS %s", HISTORIC_DATABASE);
        jdbcTemplate.execute(createDbSql);

        // Contar registros antes de archivar
        long recordsBefore = getTableRecordCount(tableName);
        logger.info("📊 Registros a archivar de {}: {}", tableName, recordsBefore);

        // Crear tabla en BD histórica si no existe
        String createSql = String.format(
                "CREATE TABLE IF NOT EXISTS %s.%s LIKE %s.%s",
                HISTORIC_DATABASE, archivedTableName, MAIN_DATABASE, tableName);
        jdbcTemplate.execute(createSql);
        logger.info("✓ Tabla histórica creada: {}.{}", HISTORIC_DATABASE, archivedTableName);

        // Copiar datos
        String insertSql = String.format(
                "INSERT INTO %s.%s SELECT * FROM %s.%s",
                HISTORIC_DATABASE, archivedTableName, MAIN_DATABASE, tableName);
        jdbcTemplate.execute(insertSql);
        logger.info("✓ Datos copiados a tabla histórica");

        // Verificar que los datos se copiaron correctamente
        String countSql = String.format("SELECT COUNT(*) FROM %s.%s", HISTORIC_DATABASE, archivedTableName);
        Long recordsArchived = jdbcTemplate.queryForObject(countSql, Long.class);
        logger.info("📊 Registros en tabla histórica: {}", recordsArchived);

        // Limpiar la tabla original después de verificar
        String truncateSql = String.format("TRUNCATE TABLE %s.%s", MAIN_DATABASE, tableName);
        jdbcTemplate.execute(truncateSql);
        logger.info("✓ Tabla original {} limpiada (TRUNCATE)", tableName);

        logger.info("✅ Tabla {} archivada exitosamente. {} registros movidos a {}.{}",
                tableName, recordsArchived, HISTORIC_DATABASE, archivedTableName);
    }

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

    private void insertNotification(String type, String title, String message) {
        try {
            String sql = "INSERT INTO notificaciones_sistema (tipo, titulo, mensaje) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, type, title, message);
        } catch (Exception e) {
            logger.warn("No se pudo insertar notificación: {}", e.getMessage());
        }
    }
}
