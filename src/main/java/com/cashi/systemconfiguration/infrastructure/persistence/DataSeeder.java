package com.cashi.systemconfiguration.infrastructure.persistence;

import com.cashi.systemconfiguration.domain.model.aggregates.Campaign;
import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationCatalog;
import com.cashi.systemconfiguration.domain.model.entities.ContactClassification;
import com.cashi.systemconfiguration.domain.model.entities.ManagementClassification;
import com.cashi.systemconfiguration.domain.model.entities.TenantClassificationConfig;
import com.cashi.systemconfiguration.domain.model.enums.ContactClassificationEnum;
import com.cashi.systemconfiguration.domain.model.enums.ManagementClassificationEnum;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.ClassificationCatalogRepository;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.LegacyCampaignRepository;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.LegacyContactClassificationRepository;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.LegacyManagementClassificationRepository;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.TenantClassificationConfigRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final LegacyContactClassificationRepository contactClassificationRepository;
    private final LegacyManagementClassificationRepository managementClassificationRepository;
    private final LegacyCampaignRepository campaignRepository;
    private final TenantRepository tenantRepository;
    private final ClassificationCatalogRepository classificationCatalogRepository;
    private final TenantClassificationConfigRepository tenantClassificationConfigRepository;
    private final PortfolioRepository portfolioRepository;

    public DataSeeder(
            LegacyContactClassificationRepository contactClassificationRepository,
            LegacyManagementClassificationRepository managementClassificationRepository,
            LegacyCampaignRepository campaignRepository,
            TenantRepository tenantRepository,
            ClassificationCatalogRepository classificationCatalogRepository,
            TenantClassificationConfigRepository tenantClassificationConfigRepository,
            PortfolioRepository portfolioRepository) {
        this.contactClassificationRepository = contactClassificationRepository;
        this.managementClassificationRepository = managementClassificationRepository;
        this.campaignRepository = campaignRepository;
        this.tenantRepository = tenantRepository;
        this.classificationCatalogRepository = classificationCatalogRepository;
        this.tenantClassificationConfigRepository = tenantClassificationConfigRepository;
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    public void run(String... args) {
        logger.info("Starting data seeding...");
        seedTenants();
        seedPortfolios();
        seedContactClassifications();
        seedManagementClassifications();
        seedClassifications();
        migrateLegacyManagementClassifications(); // MIGRAR clasificaciones antiguas al nuevo catálogo
        seedTenantConfigurations();
        seedCampaigns();
        logger.info("Data seeding completed successfully!");
    }

    private void seedTenants() {
        logger.info("Seeding Tenants...");

        // Tenant de Prueba
        if (!tenantRepository.existsByTenantCode("TENANT001")) {
            Tenant tenant = new Tenant("TENANT001", "Tenant de Prueba");
            tenant.setBusinessName("Empresa Prueba SAC");
            tenant.setIsActive(true);
            tenantRepository.save(tenant);
            logger.info("Created Tenant: TENANT001 - Tenant de Prueba");
        }

        // Financiera Oh
        if (!tenantRepository.existsByTenantCode("FIN-OH")) {
            Tenant tenant = new Tenant("FIN-OH", "Financiera Oh");
            tenant.setBusinessName("Financiera Oh S.A.");
            tenant.setIsActive(true);
            tenantRepository.save(tenant);
            logger.info("Created Tenant: FIN-OH - Financiera Oh");
        }
    }

    private void seedPortfolios() {
        logger.info("Seeding Portfolios...");

        // PORTFOLIOS PARA TENANT001 (Tenant de Prueba)
        Tenant tenant001 = tenantRepository.findByTenantCode("TENANT001").orElse(null);
        if (tenant001 != null) {
            // Subcartera 1: Tarjetas de Crédito
            if (!portfolioRepository.existsByTenantAndPortfolioCode(tenant001, "PF-TC")) {
                Portfolio portfolioTC = new Portfolio(tenant001, "PF-TC", "Tarjetas de Crédito", Portfolio.PortfolioType.CREDIT_CARD);
                portfolioTC.setDescription("Subcartera de gestión de tarjetas de crédito vencidas");
                portfolioRepository.save(portfolioTC);
                logger.info("Created Portfolio: PF-TC - Tarjetas de Crédito for TENANT001");
            }

            // Subcartera 2: Préstamos Personales
            if (!portfolioRepository.existsByTenantAndPortfolioCode(tenant001, "PF-PP")) {
                Portfolio portfolioPP = new Portfolio(tenant001, "PF-PP", "Préstamos Personales", Portfolio.PortfolioType.PERSONAL_LOAN);
                portfolioPP.setDescription("Subcartera de gestión de préstamos personales en mora");
                portfolioRepository.save(portfolioPP);
                logger.info("Created Portfolio: PF-PP - Préstamos Personales for TENANT001");
            }

            // Subcartera 3: Préstamos Hipotecarios
            if (!portfolioRepository.existsByTenantAndPortfolioCode(tenant001, "PF-PH")) {
                Portfolio portfolioPH = new Portfolio(tenant001, "PF-PH", "Préstamos Hipotecarios", Portfolio.PortfolioType.MORTGAGE);
                portfolioPH.setDescription("Subcartera de gestión de préstamos hipotecarios");
                portfolioRepository.save(portfolioPH);
                logger.info("Created Portfolio: PF-PH - Préstamos Hipotecarios for TENANT001");
            }
        }

        // PORTFOLIOS PARA FINANCIERA OH
        Tenant finOh = tenantRepository.findByTenantCode("FIN-OH").orElse(null);
        if (finOh != null) {
            // Tramo 3
            if (!portfolioRepository.existsByTenantAndPortfolioCode(finOh, "TRAMO-3")) {
                Portfolio tramo3 = new Portfolio(finOh, "TRAMO-3", "Tramo 3", Portfolio.PortfolioType.PERSONAL_LOAN);
                tramo3.setDescription("Cartera de cobranza - Tramo 3 (61-90 días de mora)");
                portfolioRepository.save(tramo3);
                logger.info("Created Portfolio: TRAMO-3 - Tramo 3 for Financiera Oh");
            }

            // Tramo 5
            if (!portfolioRepository.existsByTenantAndPortfolioCode(finOh, "TRAMO-5")) {
                Portfolio tramo5 = new Portfolio(finOh, "TRAMO-5", "Tramo 5", Portfolio.PortfolioType.PERSONAL_LOAN);
                tramo5.setDescription("Cartera de cobranza - Tramo 5 (121+ días de mora)");
                portfolioRepository.save(tramo5);
                logger.info("Created Portfolio: TRAMO-5 - Tramo 5 for Financiera Oh");
            }
        } else {
            logger.warn("Tenant FIN-OH not found, skipping Financiera Oh portfolio seeding");
        }
    }

    private void seedClassifications() {
        logger.info("Seeding Classification Catalog...");

        // Nivel 1: Resultados de Contacto
        if (!classificationCatalogRepository.existsByCode("CPC")) {
            ClassificationCatalog cpc = new ClassificationCatalog(
                    "CPC",
                    "Contacto con Cliente",
                    ClassificationCatalog.ClassificationType.CONTACT_RESULT
            );
            cpc.setIconName("phone");
            cpc.setColorHex("#4CAF50");
            cpc.setDisplayOrder(1);
            classificationCatalogRepository.save(cpc);
            logger.info("Created Classification: CPC - Contacto con Cliente");
        }

        if (!classificationCatalogRepository.existsByCode("SNC")) {
            ClassificationCatalog snc = new ClassificationCatalog(
                    "SNC",
                    "Sin Contacto",
                    ClassificationCatalog.ClassificationType.CONTACT_RESULT
            );
            snc.setIconName("phone_missed");
            snc.setColorHex("#F44336");
            snc.setDisplayOrder(2);
            classificationCatalogRepository.save(snc);
            logger.info("Created Classification: SNC - Sin Contacto");
        }

        // Nivel 1: Tipos de Gestión
        if (!classificationCatalogRepository.existsByCode("CTT")) {
            ClassificationCatalog ctt = new ClassificationCatalog(
                    "CTT",
                    "Contacto Telefónico",
                    ClassificationCatalog.ClassificationType.MANAGEMENT_TYPE
            );
            ctt.setIconName("call");
            ctt.setColorHex("#2196F3");
            ctt.setDisplayOrder(1);
            classificationCatalogRepository.save(ctt);
            logger.info("Created Classification: CTT - Contacto Telefónico");
        }

        // Nivel 2: Tipos de Pago (hijos de CPC)
        ClassificationCatalog cpc = classificationCatalogRepository.findByCode("CPC").orElse(null);
        if (cpc != null) {
            if (!classificationCatalogRepository.existsByCode("ACP")) {
                ClassificationCatalog acp = new ClassificationCatalog(
                        "ACP",
                        "Acuerdo de Pago",
                        ClassificationCatalog.ClassificationType.PAYMENT_TYPE
                );
                acp.setParentClassification(cpc);
                acp.setIconName("handshake");
                acp.setColorHex("#FF9800");
                acp.setDisplayOrder(1);
                classificationCatalogRepository.save(acp);
                logger.info("Created Classification: ACP - Acuerdo de Pago (child of CPC)");
            }

            if (!classificationCatalogRepository.existsByCode("RCP")) {
                ClassificationCatalog rcp = new ClassificationCatalog(
                        "RCP",
                        "Rechazo de Pago",
                        ClassificationCatalog.ClassificationType.PAYMENT_TYPE
                );
                rcp.setParentClassification(cpc);
                rcp.setIconName("block");
                rcp.setColorHex("#E91E63");
                rcp.setDisplayOrder(2);
                classificationCatalogRepository.save(rcp);
                logger.info("Created Classification: RCP - Rechazo de Pago (child of CPC)");
            }
        }

        // Nivel 3: Métodos de Pago (hijos de ACP)
        ClassificationCatalog acp = classificationCatalogRepository.findByCode("ACP").orElse(null);
        if (acp != null) {
            if (!classificationCatalogRepository.existsByCode("PRM")) {
                ClassificationCatalog prm = new ClassificationCatalog(
                        "PRM",
                        "Promesa de Pago",
                        ClassificationCatalog.ClassificationType.PAYMENT_TYPE
                );
                prm.setParentClassification(acp);
                prm.setIconName("schedule");
                prm.setColorHex("#9C27B0");
                prm.setDisplayOrder(1);
                classificationCatalogRepository.save(prm);
                logger.info("Created Classification: PRM - Promesa de Pago (child of ACP)");
            }

            if (!classificationCatalogRepository.existsByCode("EFE")) {
                ClassificationCatalog efe = new ClassificationCatalog(
                        "EFE",
                        "Pago en Efectivo",
                        ClassificationCatalog.ClassificationType.PAYMENT_TYPE
                );
                efe.setParentClassification(acp);
                efe.setIconName("attach_money");
                efe.setColorHex("#4CAF50");
                efe.setDisplayOrder(2);
                classificationCatalogRepository.save(efe);
                logger.info("Created Classification: EFE - Pago en Efectivo (child of ACP)");
            }

            if (!classificationCatalogRepository.existsByCode("TRF")) {
                ClassificationCatalog trf = new ClassificationCatalog(
                        "TRF",
                        "Transferencia Bancaria",
                        ClassificationCatalog.ClassificationType.PAYMENT_TYPE
                );
                trf.setParentClassification(acp);
                trf.setIconName("account_balance");
                trf.setColorHex("#3F51B5");
                trf.setDisplayOrder(3);
                classificationCatalogRepository.save(trf);
                logger.info("Created Classification: TRF - Transferencia Bancaria (child of ACP)");
            }
        }
    }

    private void seedTenantConfigurations() {
        logger.info("Seeding Tenant Classification Configurations...");

        // Get all classifications
        java.util.List<ClassificationCatalog> allClassifications = classificationCatalogRepository.findAllActive();

        // CONFIGURAR TENANT001
        Tenant tenant001 = tenantRepository.findByTenantCode("TENANT001").orElse(null);
        if (tenant001 != null) {
            for (ClassificationCatalog classification : allClassifications) {
                java.util.Optional<TenantClassificationConfig> existingConfig =
                        tenantClassificationConfigRepository.findByTenantAndPortfolioAndClassification(
                                tenant001, null, classification);

                if (existingConfig.isEmpty()) {
                    TenantClassificationConfig config = new TenantClassificationConfig(
                            tenant001,
                            null, // portfolio = null for tenant-level config
                            classification,
                            true  // isEnabled = true
                    );
                    tenantClassificationConfigRepository.save(config);
                    logger.info("Enabled classification {} for tenant {}",
                            classification.getCode(), tenant001.getTenantCode());
                }
            }
        }

        // CONFIGURAR FINANCIERA OH
        Tenant finOh = tenantRepository.findByTenantCode("FIN-OH").orElse(null);
        if (finOh != null) {
            for (ClassificationCatalog classification : allClassifications) {
                java.util.Optional<TenantClassificationConfig> existingConfig =
                        tenantClassificationConfigRepository.findByTenantAndPortfolioAndClassification(
                                finOh, null, classification);

                if (existingConfig.isEmpty()) {
                    TenantClassificationConfig config = new TenantClassificationConfig(
                            finOh,
                            null, // portfolio = null for tenant-level config
                            classification,
                            true  // isEnabled = true
                    );
                    tenantClassificationConfigRepository.save(config);
                    logger.info("Enabled classification {} for tenant {}",
                            classification.getCode(), finOh.getTenantCode());
                }
            }
        } else {
            logger.warn("Tenant FIN-OH not found, skipping Financiera Oh configurations");
        }

        logger.info("Tenant configurations seeded successfully");
    }

    private void seedContactClassifications() {
        logger.info("Seeding Contact Classifications...");
        Arrays.stream(ContactClassificationEnum.values()).forEach(enumValue -> {
            if (!contactClassificationRepository.existsByCode(enumValue.getCode())) {
                ContactClassification classification = new ContactClassification(
                        enumValue.getCode(),
                        enumValue.getDescription(),
                        enumValue.getIsSuccessful()
                );
                contactClassificationRepository.save(classification);
                logger.info("Created Contact Classification: {} - {}", enumValue.getCode(), enumValue.getDescription());
            }
        });
    }

    private void seedManagementClassifications() {
        logger.info("Seeding Management Classifications...");
        Arrays.stream(ManagementClassificationEnum.values()).forEach(enumValue -> {
            if (!managementClassificationRepository.existsByCode(enumValue.getCode())) {
                ManagementClassification classification = new ManagementClassification(
                        enumValue.getCode(),
                        enumValue.getDescription(),
                        enumValue.getRequiresPayment(),
                        enumValue.getRequiresSchedule(),
                        false // requiresFollowUp - por defecto false
                );
                managementClassificationRepository.save(classification);
                logger.info("Created Management Classification: {} - {}", enumValue.getCode(), enumValue.getDescription());
            }
        });
    }

    private void seedCampaigns() {
        logger.info("Seeding Campaigns...");

        if (!campaignRepository.existsByCampaignId("CAMP-001")) {
            Campaign campaign1 = new Campaign("CAMP-001", "Cartera Vencida Q1 2025", "Cobranza Temprana");
            campaign1.activate();
            campaignRepository.save(campaign1);
            logger.info("Created Campaign: CAMP-001");
        }

        if (!campaignRepository.existsByCampaignId("CAMP-002")) {
            Campaign campaign2 = new Campaign("CAMP-002", "Cartera Judicial Q1 2025", "Cobranza Judicial");
            campaign2.activate();
            campaignRepository.save(campaign2);
            logger.info("Created Campaign: CAMP-002");
        }

        if (!campaignRepository.existsByCampaignId("CAMP-003")) {
            Campaign campaign3 = new Campaign("CAMP-003", "Refinanciamiento Q1 2025", "Refinanciamiento");
            campaign3.activate();
            campaignRepository.save(campaign3);
            logger.info("Created Campaign: CAMP-003");
        }
    }

    /**
     * MIGRACIÓN: Copia las clasificaciones antiguas de management_classifications
     * al nuevo catálogo unificado (classification_catalog)
     */
    private void migrateLegacyManagementClassifications() {
        logger.info("========================================");
        logger.info("MIGRATING Legacy Management Classifications to New Catalog");
        logger.info("========================================");

        // Obtener todas las clasificaciones antiguas
        var legacyClassifications = managementClassificationRepository.findAll();
        int migrated = 0;
        int skipped = 0;

        for (ManagementClassification legacy : legacyClassifications) {
            // Verificar si ya existe en el nuevo catálogo
            if (classificationCatalogRepository.existsByCode(legacy.getCode())) {
                logger.debug("Skipping {} - already exists in new catalog", legacy.getCode());
                skipped++;
                continue;
            }

            // Crear nueva clasificación en el catálogo
            ClassificationCatalog newClassification = new ClassificationCatalog(
                legacy.getCode(),
                legacy.getLabel(),
                ClassificationCatalog.ClassificationType.MANAGEMENT_TYPE
            );

            // Mapear colores basados en tipo
            String color = determineColorForManagementType(legacy);
            newClassification.setColorHex(color);

            // Mapear iconos basados en tipo
            String icon = determineIconForManagementType(legacy);
            newClassification.setIconName(icon);

            // Crear metadata JSON con los campos legacy
            String metadata = String.format(
                "{\"requiresPayment\":%b,\"requiresSchedule\":%b,\"requiresFollowUp\":%b}",
                legacy.getRequiresPayment(),
                legacy.getRequiresSchedule(),
                legacy.getRequiresFollowUp()
            );
            newClassification.setMetadataSchema(metadata);

            // Marcar como sistema (no editable fácilmente)
            newClassification.setIsSystem(false); // Permitir edición para que el usuario pueda personalizar

            // Orden de visualización
            newClassification.setDisplayOrder(migrated * 10);

            // Guardar
            classificationCatalogRepository.save(newClassification);
            logger.info("✓ Migrated: {} - {}", legacy.getCode(), legacy.getLabel());
            migrated++;
        }

        logger.info("========================================");
        logger.info("Migration Summary:");
        logger.info("  - Migrated: {} classifications", migrated);
        logger.info("  - Skipped (already exist): {}", skipped);
        logger.info("  - Total Legacy: {}", legacyClassifications.size());
        logger.info("========================================");
    }

    /**
     * Determina el color apropiado basado en el tipo de gestión
     */
    private String determineColorForManagementType(ManagementClassification legacy) {
        String code = legacy.getCode();

        // Pagos exitosos - Verde
        if (code.startsWith("PG") || code.equals("ACP") || code.equals("PPR")) {
            return "#10B981"; // Verde
        }

        // Pagos parciales o convenios - Naranja
        if (code.equals("PGP") || code.startsWith("CNV") || code.startsWith("C")) {
            return "#F59E0B"; // Naranja
        }

        // Solicitudes - Azul
        if (code.startsWith("S")) {
            return "#3B82F6"; // Azul
        }

        // Problemas o rechazos - Rojo
        if (code.startsWith("N") || code.startsWith("D") || code.equals("RCL") || code.equals("FRD")) {
            return "#EF4444"; // Rojo
        }

        // Clientes agresivos o problemas - Púrpura
        if (code.equals("AGR") || code.equals("NBL") || code.equals("LGL")) {
            return "#8B5CF6"; // Púrpura
        }

        // Por defecto - Gris
        return "#64748B";
    }

    /**
     * Determina el icono apropiado basado en el tipo de gestión
     */
    private String determineIconForManagementType(ManagementClassification legacy) {
        String code = legacy.getCode();

        // Pagos
        if (code.startsWith("PG") || code.equals("PPR")) {
            return "dollar-sign";
        }

        // Acuerdos y convenios
        if (code.equals("ACP") || code.startsWith("CNV") || code.startsWith("C")) {
            return "handshake";
        }

        // Solicitudes
        if (code.startsWith("S")) {
            return "message-square";
        }

        // Disputas y problemas
        if (code.startsWith("D") || code.startsWith("N")) {
            return "alert-circle";
        }

        // Reclamos
        if (code.equals("RCL") || code.equals("FRD")) {
            return "alert-triangle";
        }

        // Calendario (requiere agendamiento)
        if (legacy.getRequiresSchedule()) {
            return "calendar";
        }

        // Agresivo
        if (code.equals("AGR") || code.equals("NBL") || code.equals("LGL")) {
            return "shield-alert";
        }

        // Por defecto
        return "file-text";
    }
}
