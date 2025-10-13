package com.cashi.systemconfiguration.infrastructure.persistence;

import com.cashi.shared.domain.model.entities.FieldTypeCatalog;
import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationCatalog;
import com.cashi.systemconfiguration.domain.model.entities.TenantClassificationConfig;
import com.cashi.systemconfiguration.domain.model.enums.ContactClassificationEnum;
import com.cashi.systemconfiguration.domain.model.enums.ManagementClassificationEnum;
import com.cashi.systemconfiguration.domain.model.enums.FinancieraOhClassificationEnum;
import com.cashi.systemconfiguration.domain.model.enums.TenantClassificationStrategy;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.ClassificationCatalogRepository;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.TenantClassificationConfigRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.FieldTypeCatalogRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final TenantRepository tenantRepository;
    private final ClassificationCatalogRepository classificationCatalogRepository;
    private final TenantClassificationConfigRepository tenantClassificationConfigRepository;
    private final PortfolioRepository portfolioRepository;
    private final FieldTypeCatalogRepository fieldTypeCatalogRepository;

    // Mapa de estrategias por tenant
    private static final Map<String, TenantClassificationStrategy> TENANT_STRATEGIES = Map.of(
        "TENANT001", TenantClassificationStrategy.GENERIC_ONLY,
        "FIN-OH", TenantClassificationStrategy.CUSTOM_ONLY
    );

    // Mapa de códigos de tenant a sus clasificaciones custom específicas
    private static final Map<String, Set<String>> TENANT_CUSTOM_CODES = Map.of(
        "FIN-OH", Set.of(
            // Nivel 1
            "RP", "CSA", "SC", "GA",
            // Nivel 2
            "PC", "PDP", "EA", "CN", "CD", "PI", "SR", "NI", "AD", "ESC",
            // Nivel 3
            "PT", "PP", "PPT", "PU", "PF", "CF",
            "EA_ENF", "EA_DES", "EA_LEG",
            "CN_RN", "CN_NIP", "CN_CI",
            "CD_SDA", "CD_SMT", "CD_RR",
            "SR_NC", "SR_BV", "SR_OC",
            "NI_AFS", "NI_NE", "NI_NEX",
            "AD_NT", "AD_ND", "AD_NC",
            "ESC_LEG", "ESC_SUP", "ESC_JUD"
        )
    );

    public DataSeeder(
            TenantRepository tenantRepository,
            ClassificationCatalogRepository classificationCatalogRepository,
            TenantClassificationConfigRepository tenantClassificationConfigRepository,
            PortfolioRepository portfolioRepository,
            FieldTypeCatalogRepository fieldTypeCatalogRepository) {
        this.tenantRepository = tenantRepository;
        this.classificationCatalogRepository = classificationCatalogRepository;
        this.tenantClassificationConfigRepository = tenantClassificationConfigRepository;
        this.portfolioRepository = portfolioRepository;
        this.fieldTypeCatalogRepository = fieldTypeCatalogRepository;
    }

    @Override
    public void run(String... args) {
        logger.info("====================================================================");
        logger.info("INICIANDO DATA SEEDING - SISTEMA MULTI-TENANT");
        logger.info("====================================================================");

        // Seed field types first (required by classifications)
        seedFieldTypes();

        seedTenants();
        seedPortfolios();

        // Clasificaciones genéricas (compartidas)
        seedContactClassifications();
        seedManagementClassifications();

        // Clasificaciones custom por tenant
        seedFinancieraOhClassifications();

        // IMPORTANTE: Limpiar configuraciones existentes antes de aplicar estrategias
        cleanExistingConfigurations();

        // Configurar tenants según estrategia
        seedTenantConfigurations();

        logger.info("====================================================================");
        logger.info("DATA SEEDING COMPLETADO EXITOSAMENTE");
        logger.info("====================================================================");
    }

    private void seedFieldTypes() {
        logger.info("--------------------------------------------------------------------");
        logger.info("Seeding Field Types Catalog...");

        // Text
        seedFieldTypeIfNotExists("text", "Texto", "Campo de texto simple de una línea",
            "type", true, true, 1);

        // Text Area
        seedFieldTypeIfNotExists("textarea", "Área de Texto", "Campo de texto multilínea",
            "align-left", true, true, 2);

        // Number
        seedFieldTypeIfNotExists("number", "Número", "Campo numérico entero",
            "hash", true, true, 3);

        // Decimal
        seedFieldTypeIfNotExists("decimal", "Decimal", "Campo numérico con decimales",
            "calculator", true, true, 4);

        // Currency
        seedFieldTypeIfNotExists("currency", "Moneda", "Campo para valores monetarios",
            "dollar-sign", true, true, 5);

        // Date
        seedFieldTypeIfNotExists("date", "Fecha", "Selector de fecha",
            "calendar", true, true, 6);

        // DateTime
        seedFieldTypeIfNotExists("datetime", "Fecha y Hora", "Selector de fecha y hora",
            "calendar-clock", true, true, 7);

        // Checkbox
        seedFieldTypeIfNotExists("checkbox", "Checkbox", "Casilla de verificación verdadero/falso",
            "check-square", true, true, 8);

        // Select
        seedFieldTypeIfNotExists("select", "Lista Desplegable", "Lista de opciones de selección única",
            "chevron-down", true, true, 9);

        // Multi-Select
        seedFieldTypeIfNotExists("multiselect", "Selección Múltiple", "Lista de opciones de selección múltiple",
            "list-checks", true, true, 10);

        // Email
        seedFieldTypeIfNotExists("email", "Email", "Campo para direcciones de correo electrónico",
            "mail", true, true, 11);

        // Phone
        seedFieldTypeIfNotExists("phone", "Teléfono", "Campo para números telefónicos",
            "phone", true, true, 12);

        // URL
        seedFieldTypeIfNotExists("url", "URL", "Campo para direcciones web",
            "link", true, true, 13);

        // JSON
        seedFieldTypeIfNotExists("json", "JSON", "Campo para datos estructurados en formato JSON",
            "braces", true, false, 14);

        // Table
        seedFieldTypeIfNotExists("table", "Tabla/Cronograma", "Tabla dinámica con filas y columnas configurables",
            "table-2", true, false, 15);

        // Auto-Number (solo para columnas de tabla)
        seedFieldTypeIfNotExists("auto-number", "Autonumérico", "Número secuencial automático (solo para tablas)",
            "list-ordered", false, true, 16);

        logger.info("✓ Field Types Catalog seeding completed");
    }

    private void seedFieldTypeIfNotExists(String typeCode, String typeName, String description,
                                          String icon, Boolean availableForMain, Boolean availableForTable,
                                          Integer displayOrder) {
        if (!fieldTypeCatalogRepository.findByTypeCode(typeCode).isPresent()) {
            FieldTypeCatalog fieldType = new FieldTypeCatalog(
                typeCode, typeName, description, icon,
                availableForMain, availableForTable, displayOrder
            );
            fieldTypeCatalogRepository.save(fieldType);
            logger.info("  ✓ {} - {}", typeCode, typeName);
        }
    }

    private void seedTenants() {
        logger.info("Seeding Tenants...");

        if (!tenantRepository.existsByTenantCode("TENANT001")) {
            Tenant tenant = new Tenant("TENANT001", "Tenant de Prueba");
            tenant.setBusinessName("Empresa Prueba SAC");
            tenant.setIsActive(true);
            tenantRepository.save(tenant);
            logger.info("✓ Created Tenant: TENANT001");
        }

        if (!tenantRepository.existsByTenantCode("FIN-OH")) {
            Tenant tenant = new Tenant("FIN-OH", "Financiera Oh");
            tenant.setBusinessName("Financiera Oh S.A.");
            tenant.setIsActive(true);
            tenantRepository.save(tenant);
            logger.info("✓ Created Tenant: FIN-OH");
        }
    }

    private void seedPortfolios() {
        logger.info("Seeding Portfolios...");

        Tenant tenant001 = tenantRepository.findByTenantCode("TENANT001").orElse(null);
        if (tenant001 != null) {
            seedPortfolioIfNotExists(tenant001, "PF-TC", "Tarjetas de Crédito",
                Portfolio.PortfolioType.CREDIT_CARD, "Subcartera de gestión de tarjetas de crédito vencidas");
            seedPortfolioIfNotExists(tenant001, "PF-PP", "Préstamos Personales",
                Portfolio.PortfolioType.PERSONAL_LOAN, "Subcartera de gestión de préstamos personales en mora");
            seedPortfolioIfNotExists(tenant001, "PF-PH", "Préstamos Hipotecarios",
                Portfolio.PortfolioType.MORTGAGE, "Subcartera de gestión de préstamos hipotecarios");
        }

        Tenant finOh = tenantRepository.findByTenantCode("FIN-OH").orElse(null);
        if (finOh != null) {
            seedPortfolioIfNotExists(finOh, "TRAMO-3", "Tramo 3",
                Portfolio.PortfolioType.PERSONAL_LOAN, "Cartera de cobranza - Tramo 3 (61-90 días de mora)");
            seedPortfolioIfNotExists(finOh, "TRAMO-5", "Tramo 5",
                Portfolio.PortfolioType.PERSONAL_LOAN, "Cartera de cobranza - Tramo 5 (121+ días de mora)");
        }
    }

    private void seedPortfolioIfNotExists(Tenant tenant, String code, String name,
                                          Portfolio.PortfolioType type, String description) {
        if (!portfolioRepository.existsByTenantAndPortfolioCode(tenant, code)) {
            Portfolio portfolio = new Portfolio(tenant, code, name, type);
            portfolio.setDescription(description);
            portfolioRepository.save(portfolio);
            logger.info("✓ Created Portfolio: {} for {}", code, tenant.getTenantCode());
        }
    }

    private void seedContactClassifications() {
        logger.info("--------------------------------------------------------------------");
        logger.info("Seeding Contact Classifications (Generic)...");

        Arrays.stream(ContactClassificationEnum.values()).forEach(enumValue -> {
            if (!classificationCatalogRepository.existsByCode(enumValue.getCode())) {
                ClassificationCatalog classification = new ClassificationCatalog(
                        enumValue.getCode(),
                        enumValue.getDescription(),
                        ClassificationCatalog.ClassificationType.CONTACT_RESULT
                );

                classification.setColorHex(enumValue.getIsSuccessful() ? "#10B981" : "#EF4444");
                classification.setIconName(enumValue.getIsSuccessful() ? "phone-call" : "phone-missed");
                classification.setIsSystem(true);
                classificationCatalogRepository.save(classification);
                logger.info("  ✓ {} - {}", enumValue.getCode(), enumValue.getDescription());
            }
        });
    }

    private void seedManagementClassifications() {
        logger.info("--------------------------------------------------------------------");
        logger.info("Seeding Management Classifications (Generic)...");

        Arrays.stream(ManagementClassificationEnum.values()).forEach(enumValue -> {
            if (!classificationCatalogRepository.existsByCode(enumValue.getCode())) {
                ClassificationCatalog classification = new ClassificationCatalog(
                        enumValue.getCode(),
                        enumValue.getDescription(),
                        ClassificationCatalog.ClassificationType.MANAGEMENT_TYPE
                );

                classification.setColorHex(determineColorForManagementType(enumValue));
                classification.setIconName(determineIconForManagementType(enumValue));

                String metadata = String.format(
                    "{\"requiresPayment\":%b,\"requiresSchedule\":%b,\"requiresFollowUp\":%b}",
                    enumValue.getRequiresPayment(),
                    enumValue.getRequiresSchedule(),
                    enumValue.getRequiresFollowUp()
                );
                classification.setMetadataSchema(metadata);
                classification.setIsSystem(true);
                classificationCatalogRepository.save(classification);
                logger.info("  ✓ {} - {}", enumValue.getCode(), enumValue.getDescription());
            }
        });
    }

    private void seedFinancieraOhClassifications() {
        logger.info("--------------------------------------------------------------------");
        logger.info("Seeding Financiera Oh Classifications (CUSTOM)...");

        Arrays.stream(FinancieraOhClassificationEnum.values()).forEach(enumValue -> {
            ClassificationCatalog classification = classificationCatalogRepository.findByCode(enumValue.getCode())
                .orElse(null);

            if (classification == null) {
                // CREAR NUEVA: Obtener el padre ANTES de crear la clasificación
                ClassificationCatalog parent = null;
                if (enumValue.getParentCode() != null) {
                    parent = classificationCatalogRepository.findByCode(enumValue.getParentCode())
                        .orElse(null);
                }

                // Usar el constructor que calcula automáticamente hierarchyLevel y hierarchyPath
                classification = new ClassificationCatalog(
                        enumValue.getCode(),
                        enumValue.getDescription(),
                        ClassificationCatalog.ClassificationType.CUSTOM,
                        parent,  // Pasar el padre para que calcule hierarchyLevel correctamente
                        null     // displayOrder
                );

                classification.setColorHex(enumValue.getColorHex());
                classification.setIconName(enumValue.getIconName());

                String metadata = String.format(
                    "{\"requiresPayment\":%b,\"requiresSchedule\":%b,\"requiresFollowUp\":%b,\"isSuccessful\":%b,\"mainCategory\":\"%s\",\"tenantCode\":\"FIN-OH\"}",
                    enumValue.requiresPayment(),
                    enumValue.requiresSchedule(),
                    enumValue.requiresFollowUp(),
                    enumValue.isSuccessful(),
                    enumValue.getMainCategory()
                );
                classification.setMetadataSchema(metadata);
                classification.setIsSystem(true);
                classificationCatalogRepository.save(classification);
                logger.info("  ✓ CREADO L{} - {} - {} (parent: {})",
                    classification.getHierarchyLevel(), enumValue.getCode(), enumValue.getDescription(),
                    parent != null ? parent.getCode() : "ROOT");
            } else {
                // ACTUALIZAR EXISTENTE: Corregir hierarchyLevel y hierarchyPath
                ClassificationCatalog parent = null;
                if (enumValue.getParentCode() != null) {
                    parent = classificationCatalogRepository.findByCode(enumValue.getParentCode())
                        .orElse(null);
                }

                classification.setParentClassification(parent);

                // Recalcular hierarchyLevel y hierarchyPath
                if (parent != null) {
                    classification.setHierarchyLevel(parent.getHierarchyLevel() + 1);
                    classification.setHierarchyPath(parent.getHierarchyPath() + "/" + classification.getCode());
                } else {
                    classification.setHierarchyLevel(1);
                    classification.setHierarchyPath("/" + classification.getCode());
                }

                classificationCatalogRepository.save(classification);
                logger.info("  ✓ ACTUALIZADO L{} - {} - {} (parent: {})",
                    classification.getHierarchyLevel(), enumValue.getCode(), enumValue.getDescription(),
                    parent != null ? parent.getCode() : "ROOT");
            }
        });
    }

    private void cleanExistingConfigurations() {
        logger.info("====================================================================");
        logger.info("LIMPIANDO CONFIGURACIONES EXISTENTES");
        logger.info("====================================================================");

        TENANT_STRATEGIES.keySet().forEach(tenantCode -> {
            tenantRepository.findByTenantCode(tenantCode).ifPresent(tenant -> {
                var existingConfigs = tenantClassificationConfigRepository.findByTenant(tenant);
                if (!existingConfigs.isEmpty()) {
                    tenantClassificationConfigRepository.deleteAll(existingConfigs);
                    logger.info("✓ Eliminadas {} configuraciones existentes de {}", existingConfigs.size(), tenantCode);
                }
            });
        });
    }

    private void seedTenantConfigurations() {
        logger.info("====================================================================");
        logger.info("CONFIGURANDO CLASIFICACIONES POR TENANT");
        logger.info("====================================================================");

        TENANT_STRATEGIES.forEach((tenantCode, strategy) -> {
            tenantRepository.findByTenantCode(tenantCode).ifPresent(tenant -> {
                logger.info("--------------------------------------------------------------------");
                logger.info("Tenant: {} | Estrategia: {}", tenantCode, strategy.getDescription());
                logger.info("--------------------------------------------------------------------");

                List<ClassificationCatalog> classificationsToEnable = new ArrayList<>();

                if (strategy.shouldLoadGeneric()) {
                    var genericClassifications = classificationCatalogRepository.findAll().stream()
                        .filter(c -> c.getClassificationType() == ClassificationCatalog.ClassificationType.CONTACT_RESULT ||
                                     c.getClassificationType() == ClassificationCatalog.ClassificationType.MANAGEMENT_TYPE)
                        .toList();
                    classificationsToEnable.addAll(genericClassifications);
                    logger.info("  → Cargando {} clasificaciones GENÉRICAS", genericClassifications.size());
                }

                if (strategy.shouldLoadCustom()) {
                    Set<String> allowedCodes = TENANT_CUSTOM_CODES.get(tenantCode);
                    if (allowedCodes != null) {
                        var customClassifications = classificationCatalogRepository.findAll().stream()
                            .filter(c -> c.getClassificationType() == ClassificationCatalog.ClassificationType.CUSTOM)
                            .filter(c -> allowedCodes.contains(c.getCode()))
                            .toList();
                        classificationsToEnable.addAll(customClassifications);
                        logger.info("  → Cargando {} clasificaciones CUSTOM específicas de {}", customClassifications.size(), tenantCode);
                    }
                }

                if (strategy.isManual()) {
                    logger.info("  → Configuración MANUAL - Sin clasificaciones automáticas");
                }

                // Habilitar solo las clasificaciones permitidas
                int enabled = 0;
                for (ClassificationCatalog classification : classificationsToEnable) {
                    TenantClassificationConfig config = new TenantClassificationConfig(
                            tenant, null, classification, true
                    );
                    tenantClassificationConfigRepository.save(config);
                    enabled++;
                }
                logger.info("  ✓ Habilitadas {} clasificaciones para {}", enabled, tenantCode);
            });
        });
    }

    private String determineColorForManagementType(ManagementClassificationEnum enumValue) {
        String code = enumValue.getCode();
        if (code.startsWith("PG") || code.equals("ACP") || code.equals("PPR")) return "#10B981";
        if (code.equals("PGP") || code.startsWith("CNV") || code.equals("REF")) return "#F59E0B";
        if (code.startsWith("S")) return "#3B82F6";
        if (code.startsWith("N") || code.startsWith("D") || code.equals("RCL") || code.equals("FRD")) return "#EF4444";
        if (code.equals("AGR") || code.equals("NBL") || code.equals("LGL")) return "#8B5CF6";
        return "#64748B";
    }

    private String determineIconForManagementType(ManagementClassificationEnum enumValue) {
        String code = enumValue.getCode();
        if (code.startsWith("PG") || code.equals("PPR")) return "dollar-sign";
        if (code.equals("ACP") || code.startsWith("CNV") || code.equals("REF")) return "handshake";
        if (code.startsWith("S")) return "message-square";
        if (code.startsWith("D") || code.startsWith("N")) return "alert-circle";
        if (code.equals("RCL") || code.equals("FRD")) return "alert-triangle";
        if (enumValue.getRequiresSchedule()) return "calendar";
        if (code.equals("AGR") || code.equals("NBL") || code.equals("LGL")) return "shield-alert";
        return "file-text";
    }
}
