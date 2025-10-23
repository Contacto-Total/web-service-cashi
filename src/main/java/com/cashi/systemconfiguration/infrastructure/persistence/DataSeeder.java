package com.cashi.systemconfiguration.infrastructure.persistence;

import com.cashi.shared.domain.model.entities.FieldTypeCatalog;
import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.systemconfiguration.domain.model.entities.TypificationCatalog;
import com.cashi.systemconfiguration.domain.model.entities.TypificationCategory;
import com.cashi.systemconfiguration.domain.model.enums.ContactTypificationEnum;
import com.cashi.systemconfiguration.domain.model.enums.ManagementTypificationEnum;
import com.cashi.systemconfiguration.domain.model.enums.FinancieraOhTypificationEnum;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.TypificationCatalogRepository;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.TypificationCategoryRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.FieldTypeCatalogRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.SubPortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final TenantRepository tenantRepository;
    private final TypificationCatalogRepository classificationCatalogRepository;
    private final TypificationCategoryRepository typificationCategoryRepository;
    private final PortfolioRepository portfolioRepository;
    private final SubPortfolioRepository subPortfolioRepository;
    private final FieldTypeCatalogRepository fieldTypeCatalogRepository;

    // Mapa de códigos de tenant a sus clasificaciones custom específicas
    private static final Map<String, Set<String>> TENANT_CUSTOM_CODES = Map.of(
        "FOH", Set.of(
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
            TypificationCatalogRepository classificationCatalogRepository,
            TypificationCategoryRepository typificationCategoryRepository,
            PortfolioRepository portfolioRepository,
            SubPortfolioRepository subPortfolioRepository,
            FieldTypeCatalogRepository fieldTypeCatalogRepository) {
        this.tenantRepository = tenantRepository;
        this.classificationCatalogRepository = classificationCatalogRepository;
        this.typificationCategoryRepository = typificationCategoryRepository;
        this.portfolioRepository = portfolioRepository;
        this.subPortfolioRepository = subPortfolioRepository;
        this.fieldTypeCatalogRepository = fieldTypeCatalogRepository;
    }

    @Override
    public void run(String... args) {
        logger.info("====================================================================");
        logger.info("INICIANDO DATA SEEDING - SISTEMA MULTI-TENANT");
        logger.info("====================================================================");

        // Seed typification types first
        seedClassificationTypes();

        // Seed field types (required by typifications)
        seedFieldTypes();

        seedTenants();
        seedPortfolios();
        seedSubPortfolios();

        // Clasificaciones genéricas (compartidas)
        seedContactClassifications();
        seedManagementTypifications();

        // Clasificaciones custom por tenant
        seedFinancieraOhClassifications();

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

        // Time
        seedFieldTypeIfNotExists("time", "Hora", "Selector de hora",
            "clock", true, true, 7);

        // DateTime
        seedFieldTypeIfNotExists("datetime", "Fecha y Hora", "Selector de fecha y hora",
            "calendar-clock", true, true, 8);

        // Checkbox
        seedFieldTypeIfNotExists("checkbox", "Checkbox", "Casilla de verificación verdadero/falso",
            "check-square", true, true, 9);

        // Select
        seedFieldTypeIfNotExists("select", "Lista Desplegable", "Lista de opciones de selección única",
            "chevron-down", true, true, 10);

        // Multi-Select
        seedFieldTypeIfNotExists("multiselect", "Selección Múltiple", "Lista de opciones de selección múltiple",
            "list-checks", true, true, 11);

        // Email
        seedFieldTypeIfNotExists("email", "Email", "Campo para direcciones de correo electrónico",
            "mail", true, true, 12);

        // Phone
        seedFieldTypeIfNotExists("phone", "Teléfono", "Campo para números telefónicos",
            "phone", true, true, 13);

        // URL
        seedFieldTypeIfNotExists("url", "URL", "Campo para direcciones web",
            "link", true, true, 14);

        // JSON
        seedFieldTypeIfNotExists("json", "JSON", "Campo para datos estructurados en formato JSON",
            "braces", true, false, 15);

        // Table
        seedFieldTypeIfNotExists("table", "Tabla/Cronograma", "Tabla dinámica con filas y columnas configurables",
            "table-2", true, false, 16);

        // Auto-Number (solo para columnas de tabla)
        seedFieldTypeIfNotExists("auto-number", "Autonumérico", "Número secuencial automático (solo para tablas)",
            "list-ordered", false, true, 17);

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

    private void seedClassificationTypes() {
        logger.info("--------------------------------------------------------------------");
        logger.info("Seeding Classification Types Catalog...");

        // Categoría PAGO
        seedClassificationTypeIfNotExists("PAGO", "TOTAL", "Pago Total", "PT",
            "Pagar todo el saldo pendiente del cronograma",
            "El cliente realiza un pago que cubre la totalidad de su deuda pendiente",
            true, false, false);

        seedClassificationTypeIfNotExists("PAGO", "PARCIAL", "Pago Parcial", "PP",
            "Pagar solo la próxima cuota del cronograma",
            "El cliente realiza un pago que cubre solo la siguiente cuota programada",
            false, true, false);

        seedClassificationTypeIfNotExists("PAGO", "PARCIAL_MULTIPLE", "Pago Parcial Múltiple", "PPM",
            "Seleccionar varias cuotas para pagar",
            "El cliente realiza un pago que cubre varias cuotas específicas del cronograma",
            false, true, false);

        seedClassificationTypeIfNotExists("PAGO", "PERSONALIZADO", "Monto Personalizado", "PX",
            "Ingresar un monto personalizado",
            "El cliente realiza un pago con un monto específico que no corresponde exactamente a las cuotas programadas",
            false, false, true);

        // Categoría CRONOGRAMA
        seedClassificationTypeIfNotExists("CRONOGRAMA", "FINANCIERA", "Cronograma Financiero", "CF",
            "Cronograma con condiciones financieras formales",
            "Cronograma de pagos con términos financieros establecidos y validados por el área correspondiente",
            false, false, false);

        seedClassificationTypeIfNotExists("CRONOGRAMA", "CONFIANZA", "Cronograma de Confianza", "CC",
            "Cronograma basado en acuerdo de palabra",
            "Cronograma de pagos establecido mediante compromiso verbal con el cliente",
            false, false, false);

        // Categoría RECLAMO
        seedClassificationTypeIfNotExists("RECLAMO", "PRODUCTO", "Reclamo de Producto", "RP",
            "Reclamo sobre calidad o características del producto",
            "El cliente presenta una queja relacionada con las características, calidad o funcionamiento del producto financiero adquirido",
            false, false, false);

        seedClassificationTypeIfNotExists("RECLAMO", "SERVICIO", "Reclamo de Servicio", "RS",
            "Reclamo sobre atención o servicio recibido",
            "El cliente presenta una queja sobre la atención recibida o el servicio prestado por la institución",
            false, false, false);

        // Categoría CONTACTO
        seedClassificationTypeIfNotExists("CONTACTO", "EXITOSO", "Contacto Exitoso", "CE",
            "Se logró contactar con el cliente",
            "Se estableció comunicación efectiva con el cliente objetivo",
            false, false, false);

        seedClassificationTypeIfNotExists("CONTACTO", "NO_CONTACTADO", "No Contactado", "NC",
            "No se logró contactar con el cliente",
            "No fue posible establecer comunicación con el cliente en el intento realizado",
            false, false, false);

        logger.info("✓ Classification Types Catalog seeding completed");
    }

    private void seedClassificationTypeIfNotExists(String category, String code, String name, String shortName,
                                                   String description, String userDescription,
                                                   Boolean suggestsFullAmount, Boolean allowsInstallmentSelection,
                                                   Boolean requiresManualAmount) {
        if (!typificationCategoryRepository.findByCategoryAndCode(category, code).isPresent()) {
            TypificationCategory type = new TypificationCategory(
                category, code, name, shortName, description, userDescription
            );
            type.setSuggestsFullAmount(suggestsFullAmount);
            type.setAllowsInstallmentSelection(allowsInstallmentSelection);
            type.setRequiresManualAmount(requiresManualAmount);
            type.setRequiresObservations(false);
            type.setAllowsFileAttachment(false);
            typificationCategoryRepository.save(type);
            logger.info("  ✓ [{} - {}] {}", category, code, name);
        }
    }

    private void seedTenants() {
        logger.info("Seeding Tenants...");

        if (!tenantRepository.existsByTenantCode("T01")) {
            Tenant tenant = new Tenant("T01", "Tenant de Prueba");
            tenant.setBusinessName("Empresa Prueba SAC");
            tenant.setIsActive(1);
            tenantRepository.save(tenant);
            logger.info("✓ Created Tenant: T01");
        }

        if (!tenantRepository.existsByTenantCode("FOH")) {
            Tenant tenant = new Tenant("FOH", "Financiera Oh");
            tenant.setBusinessName("Financiera Oh S.A.");
            tenant.setIsActive(1);
            tenantRepository.save(tenant);
            logger.info("✓ Created Tenant: FOH");
        }
    }

    private void seedPortfolios() {
        logger.info("Seeding Portfolios...");

        Tenant tenant001 = tenantRepository.findByTenantCode("T01").orElse(null);
        if (tenant001 != null) {
            seedPortfolioIfNotExists(tenant001, "TC", "Tarjetas de Crédito",
                "Subcartera de gestión de tarjetas de crédito vencidas");
            seedPortfolioIfNotExists(tenant001, "PP", "Préstamos Personales",
                "Subcartera de gestión de préstamos personales en mora");
            seedPortfolioIfNotExists(tenant001, "PH", "Préstamos Hipotecarios",
                "Subcartera de gestión de préstamos hipotecarios");
        }

        Tenant finOh = tenantRepository.findByTenantCode("FOH").orElse(null);
        if (finOh != null) {
            seedPortfolioIfNotExists(finOh, "TR3", "Tramo 3",
                "Cartera de cobranza - Tramo 3 (61-90 días de mora)");
            seedPortfolioIfNotExists(finOh, "TR5", "Tramo 5",
                "Cartera de cobranza - Tramo 5 (121+ días de mora)");
        }
    }

    private void seedPortfolioIfNotExists(Tenant tenant, String code, String name, String description) {
        if (!portfolioRepository.existsByTenantAndPortfolioCode(tenant, code)) {
            Portfolio portfolio = new Portfolio(tenant, code, name);
            portfolio.setDescription(description);
            portfolioRepository.save(portfolio);
            logger.info("✓ Created Portfolio: {} for {}", code, tenant.getTenantCode());
        }
    }

    private void seedSubPortfolios() {
        logger.info("Seeding SubPortfolios...");

        Tenant tenant001 = tenantRepository.findByTenantCode("T01").orElse(null);
        if (tenant001 != null) {
            // Subcarteras para Tarjetas de Crédito
            Portfolio tcPortfolio = portfolioRepository.findByTenantAndPortfolioCode(tenant001, "TC").orElse(null);
            if (tcPortfolio != null) {
                seedSubPortfolioIfNotExists(tcPortfolio, "CLS", "TC Clásica", "Tarjetas clásicas en mora");
                seedSubPortfolioIfNotExists(tcPortfolio, "GLD", "TC Gold", "Tarjetas gold en mora");
                seedSubPortfolioIfNotExists(tcPortfolio, "PLT", "TC Platinum", "Tarjetas platinum en mora");
            }

            // Subcarteras para Préstamos Personales
            Portfolio ppPortfolio = portfolioRepository.findByTenantAndPortfolioCode(tenant001, "PP").orElse(null);
            if (ppPortfolio != null) {
                seedSubPortfolioIfNotExists(ppPortfolio, "CON", "Consumo", "Préstamos de consumo");
                seedSubPortfolioIfNotExists(ppPortfolio, "VEH", "Vehicular", "Préstamos vehiculares");
            }
        }

        Tenant finOh = tenantRepository.findByTenantCode("FOH").orElse(null);
        if (finOh != null) {
            // Subcarteras para TR3
            Portfolio tramo3 = portfolioRepository.findByTenantAndPortfolioCode(finOh, "TR3").orElse(null);
            if (tramo3 != null) {
                seedSubPortfolioIfNotExists(tramo3, "LIM", "Lima", "Clientes de Lima - Tramo 3");
                seedSubPortfolioIfNotExists(tramo3, "PRO", "Provincia", "Clientes de Provincia - Tramo 3");
            }

            // Subcarteras para TR5
            Portfolio tramo5 = portfolioRepository.findByTenantAndPortfolioCode(finOh, "TR5").orElse(null);
            if (tramo5 != null) {
                seedSubPortfolioIfNotExists(tramo5, "CAS", "Castigada", "Cartera castigada - Tramo 5");
                seedSubPortfolioIfNotExists(tramo5, "JUD", "Judicial", "Cartera en proceso judicial - Tramo 5");
            }
        }
    }

    private void seedSubPortfolioIfNotExists(Portfolio portfolio, String code, String name, String description) {
        if (!subPortfolioRepository.existsByPortfolioAndSubPortfolioCode(portfolio, code)) {
            SubPortfolio subPortfolio = new SubPortfolio(portfolio, code, name, description);
            subPortfolioRepository.save(subPortfolio);
            logger.info("✓ Created SubPortfolio: {} for Portfolio {}", code, portfolio.getPortfolioCode());
        }
    }

    private void seedContactClassifications() {
        logger.info("--------------------------------------------------------------------");
        logger.info("Seeding Contact Classifications (Generic)...");

        Arrays.stream(ContactTypificationEnum.values()).forEach(enumValue -> {
            if (!classificationCatalogRepository.existsByCode(enumValue.getCode())) {
                TypificationCatalog typification = new TypificationCatalog(
                        enumValue.getCode(),
                        enumValue.getDescription(),
                        TypificationCatalog.ClassificationType.CONTACT_RESULT
                );

                typification.setColorHex(enumValue.getIsSuccessful() ? "#10B981" : "#EF4444");
                typification.setIconName(enumValue.getIsSuccessful() ? "phone-call" : "phone-missed");
                typification.setIsSystem(1);
                classificationCatalogRepository.save(typification);
                logger.info("  ✓ {} - {}", enumValue.getCode(), enumValue.getDescription());
            }
        });
    }

    private void seedManagementTypifications() {
        logger.info("--------------------------------------------------------------------");
        logger.info("Seeding Management Classifications (Generic)...");

        Arrays.stream(ManagementTypificationEnum.values()).forEach(enumValue -> {
            if (!classificationCatalogRepository.existsByCode(enumValue.getCode())) {
                TypificationCatalog typification = new TypificationCatalog(
                        enumValue.getCode(),
                        enumValue.getDescription(),
                        TypificationCatalog.ClassificationType.MANAGEMENT_TYPE
                );

                typification.setColorHex(determineColorForManagementType(enumValue));
                typification.setIconName(determineIconForManagementType(enumValue));

                String metadata = String.format(
                    "{\"requiresPayment\":%b,\"requiresSchedule\":%b,\"requiresFollowUp\":%b}",
                    enumValue.getRequiresPayment(),
                    enumValue.getRequiresSchedule(),
                    enumValue.getRequiresFollowUp()
                );
                typification.setIsSystem(1);
                classificationCatalogRepository.save(typification);
                logger.info("  ✓ {} - {}", enumValue.getCode(), enumValue.getDescription());
            }
        });
    }

    private void seedFinancieraOhClassifications() {
        logger.info("--------------------------------------------------------------------");
        logger.info("Seeding Financiera Oh Classifications (CUSTOM)...");

        Arrays.stream(FinancieraOhTypificationEnum.values()).forEach(enumValue -> {
            TypificationCatalog typification = classificationCatalogRepository.findByCode(enumValue.getCode())
                .orElse(null);

            if (typification == null) {
                // CREAR NUEVA: Obtener el padre ANTES de crear la clasificación
                TypificationCatalog parent = null;
                if (enumValue.getParentCode() != null) {
                    parent = classificationCatalogRepository.findByCode(enumValue.getParentCode())
                        .orElse(null);
                }

                // Usar el constructor que calcula automáticamente hierarchyLevel y hierarchyPath
                typification = new TypificationCatalog(
                        enumValue.getCode(),
                        enumValue.getDescription(),
                        TypificationCatalog.ClassificationType.CUSTOM,
                        parent,  // Pasar el padre para que calcule hierarchyLevel correctamente
                        null     // displayOrder
                );

                typification.setColorHex(enumValue.getColorHex());
                typification.setIconName(enumValue.getIconName());

                // Asignar tipo de clasificación si es una tipificación de pago
                assignClassificationTypeIfApplicable(typification);

                // Metadata básico + Campos dinámicos (si aplica)
                String metadata = buildMetadataWithDynamicFields(enumValue);
                typification.setIsSystem(1);
                classificationCatalogRepository.save(typification);
                logger.info("  ✓ CREADO L{} - {} - {} (parent: {})",
                    typification.getHierarchyLevel(), enumValue.getCode(), enumValue.getDescription(),
                    parent != null ? parent.getCode() : "ROOT");
            } else {
                // ACTUALIZAR EXISTENTE: Corregir hierarchyLevel, hierarchyPath y metadata
                TypificationCatalog parent = null;
                if (enumValue.getParentCode() != null) {
                    parent = classificationCatalogRepository.findByCode(enumValue.getParentCode())
                        .orElse(null);
                }

                typification.setParentTypification(parent);

                // Recalcular hierarchyLevel y hierarchyPath
                if (parent != null) {
                    typification.setHierarchyLevel(parent.getHierarchyLevel() + 1);
                    typification.setHierarchyPath(parent.getHierarchyPath() + "/" + typification.getCode());
                } else {
                    typification.setHierarchyLevel(1);
                    typification.setHierarchyPath("/" + typification.getCode());
                }

                // Actualizar metadata con los flags correctos
                String metadata = buildMetadataWithDynamicFields(enumValue);

                // Actualizar también el color e icono
                typification.setColorHex(enumValue.getColorHex());
                typification.setIconName(enumValue.getIconName());

                // Asignar tipo de clasificación si es una tipificación de pago
                assignClassificationTypeIfApplicable(typification);

                classificationCatalogRepository.save(typification);
                logger.info("  ✓ ACTUALIZADO L{} - {} - {} (parent: {}) [metadata actualizado]",
                    typification.getHierarchyLevel(), enumValue.getCode(), enumValue.getDescription(),
                    parent != null ? parent.getCode() : "ROOT");
            }
        });
    }


    private String determineColorForManagementType(ManagementTypificationEnum enumValue) {
        String code = enumValue.getCode();
        if (code.startsWith("PG") || code.equals("ACP") || code.equals("PPR")) return "#10B981";
        if (code.equals("PGP") || code.startsWith("CNV") || code.equals("REF")) return "#F59E0B";
        if (code.startsWith("S")) return "#3B82F6";
        if (code.startsWith("N") || code.startsWith("D") || code.equals("RCL") || code.equals("FRD")) return "#EF4444";
        if (code.equals("AGR") || code.equals("NBL") || code.equals("LGL")) return "#8B5CF6";
        return "#64748B";
    }

    private String determineIconForManagementType(ManagementTypificationEnum enumValue) {
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

    /**
     * Construye el metadata JSON completo incluyendo campos dinámicos según la tipificación
     */
    private String buildMetadataWithDynamicFields(FinancieraOhTypificationEnum enumValue) {
        String baseMetadata = String.format(
            "{\"requiresPayment\":%b,\"requiresSchedule\":%b,\"requiresFollowUp\":%b,\"isSuccessful\":%b,\"mainCategory\":\"%s\",\"tenantCode\":\"FIN-OH\"",
            enumValue.requiresPayment(),
            enumValue.requiresSchedule(),
            enumValue.requiresFollowUp(),
            enumValue.isSuccessful(),
            enumValue.getMainCategory()
        );

        // Agregar campos dinámicos según el código de la tipificación
        String dynamicFields = getDynamicFieldsForClassification(enumValue.getCode());

        if (dynamicFields != null && !dynamicFields.isEmpty()) {
            return baseMetadata + ",\"fields\":" + dynamicFields + "}";
        }

        return baseMetadata + "}";
    }

    /**
     * Retorna el array JSON de campos dinámicos según el código de clasificación
     * Retorna null si no requiere campos dinámicos
     */
    private String getDynamicFieldsForClassification(String code) {
        switch (code) {
            case "PT": // Pago Total
                return "[" +
                    "{\"fieldCode\":\"monto_pagado\",\"fieldName\":\"Monto Pagado\",\"fieldType\":\"currency\",\"isRequired\":true,\"displayOrder\":1}," +
                    "{\"fieldCode\":\"metodo_pago\",\"fieldName\":\"Método de Pago\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Efectivo\",\"Transferencia\",\"Depósito\",\"Tarjeta\",\"Yape\",\"Plin\"],\"displayOrder\":2}," +
                    "{\"fieldCode\":\"numero_operacion\",\"fieldName\":\"Nº Operación\",\"fieldType\":\"text\",\"isRequired\":true,\"displayOrder\":3}," +
                    "{\"fieldCode\":\"fecha_pago\",\"fieldName\":\"Fecha de Pago\",\"fieldType\":\"date\",\"isRequired\":true,\"displayOrder\":4}" +
                "]";

            case "PP": // Pago Parcial
                return "[" +
                    "{\"fieldCode\":\"monto_pagado\",\"fieldName\":\"Monto Pagado\",\"fieldType\":\"currency\",\"isRequired\":true,\"displayOrder\":1}," +
                    "{\"fieldCode\":\"saldo_pendiente\",\"fieldName\":\"Saldo Pendiente\",\"fieldType\":\"currency\",\"isRequired\":true,\"displayOrder\":2}," +
                    "{\"fieldCode\":\"metodo_pago\",\"fieldName\":\"Método de Pago\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Efectivo\",\"Transferencia\",\"Depósito\",\"Tarjeta\",\"Yape\",\"Plin\"],\"displayOrder\":3}," +
                    "{\"fieldCode\":\"numero_operacion\",\"fieldName\":\"Nº Operación\",\"fieldType\":\"text\",\"isRequired\":true,\"displayOrder\":4}," +
                    "{\"fieldCode\":\"fecha_pago\",\"fieldName\":\"Fecha de Pago\",\"fieldType\":\"date\",\"isRequired\":true,\"displayOrder\":5}," +
                    "{\"fieldCode\":\"compromiso_saldo\",\"fieldName\":\"Compromiso Saldo\",\"fieldType\":\"date\",\"isRequired\":false,\"displayOrder\":6}" +
                "]";

            case "PPT": // Pago por Tercero
                return "[" +
                    "{\"fieldCode\":\"nombre_tercero\",\"fieldName\":\"Nombre del Tercero\",\"fieldType\":\"text\",\"isRequired\":true,\"displayOrder\":1}," +
                    "{\"fieldCode\":\"relacion\",\"fieldName\":\"Relación\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Familiar\",\"Cónyuge\",\"Aval\",\"Amigo\",\"Otro\"],\"displayOrder\":2}," +
                    "{\"fieldCode\":\"telefono_tercero\",\"fieldName\":\"Teléfono\",\"fieldType\":\"phone\",\"isRequired\":false,\"displayOrder\":3}," +
                    "{\"fieldCode\":\"monto_pagado\",\"fieldName\":\"Monto Pagado\",\"fieldType\":\"currency\",\"isRequired\":true,\"displayOrder\":4}," +
                    "{\"fieldCode\":\"metodo_pago\",\"fieldName\":\"Método de Pago\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Efectivo\",\"Transferencia\",\"Depósito\",\"Yape\",\"Plin\"],\"displayOrder\":5}," +
                    "{\"fieldCode\":\"numero_operacion\",\"fieldName\":\"Nº Operación\",\"fieldType\":\"text\",\"isRequired\":true,\"displayOrder\":6}" +
                "]";

            case "PU": // Pago Único (Promesa)
                return "[" +
                    "{\"fieldCode\":\"monto_comprometido\",\"fieldName\":\"Monto Comprometido\",\"fieldType\":\"currency\",\"isRequired\":true,\"displayOrder\":1}," +
                    "{\"fieldCode\":\"fecha_compromiso\",\"fieldName\":\"Fecha Compromiso\",\"fieldType\":\"date\",\"isRequired\":true,\"displayOrder\":2}," +
                    "{\"fieldCode\":\"hora_aproximada\",\"fieldName\":\"Hora Aproximada\",\"fieldType\":\"text\",\"isRequired\":false,\"displayOrder\":3}," +
                    "{\"fieldCode\":\"metodo_pago_prometido\",\"fieldName\":\"Método Prometido\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Efectivo\",\"Transferencia\",\"Depósito\",\"Yape\",\"Plin\",\"Agente\"],\"displayOrder\":4}," +
                    "{\"fieldCode\":\"nivel_confianza\",\"fieldName\":\"Nivel de Confianza\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Alto\",\"Medio\",\"Bajo\"],\"displayOrder\":5}" +
                "]";

            case "PF": // Pago Fraccionado
                return "[" +
                    "{\"fieldCode\":\"numero_cuotas\",\"fieldName\":\"Número de Cuotas\",\"fieldType\":\"number\",\"isRequired\":true,\"displayOrder\":1,\"min\":1,\"max\":50}," +
                    "{\"fieldCode\":\"cronograma_pagos\",\"fieldName\":\"Cronograma de Pagos\",\"fieldType\":\"table\",\"isRequired\":true,\"displayOrder\":2," +
                    "\"linkedToField\":\"numero_cuotas\"," +
                    "\"columns\":[" +
                        "{\"id\":\"cuota\",\"label\":\"#\",\"type\":\"auto-number\",\"required\":false}," +
                        "{\"id\":\"fecha_vencimiento\",\"label\":\"Fecha Vencimiento\",\"type\":\"date\",\"required\":true,\"minDate\":\"today\"}," +
                        "{\"id\":\"monto\",\"label\":\"Monto\",\"type\":\"currency\",\"required\":true}" +
                    "],\"minRows\":1,\"maxRows\":50,\"allowAddRow\":true,\"allowDeleteRow\":true}" +
                "]";

            case "CF": // Convenio Formal
                return "[" +
                    "{\"fieldCode\":\"numero_convenio\",\"fieldName\":\"Nº Convenio\",\"fieldType\":\"text\",\"isRequired\":true,\"displayOrder\":1}," +
                    "{\"fieldCode\":\"fecha_convenio\",\"fieldName\":\"Fecha Convenio\",\"fieldType\":\"date\",\"isRequired\":true,\"displayOrder\":2}," +
                    "{\"fieldCode\":\"monto_inicial\",\"fieldName\":\"Cuota Inicial\",\"fieldType\":\"currency\",\"isRequired\":false,\"displayOrder\":3}," +
                    "{\"fieldCode\":\"numero_cuotas\",\"fieldName\":\"Número de Cuotas\",\"fieldType\":\"number\",\"isRequired\":true,\"displayOrder\":4}," +
                    "{\"fieldCode\":\"monto_cuota\",\"fieldName\":\"Monto por Cuota\",\"fieldType\":\"currency\",\"isRequired\":true,\"displayOrder\":5}," +
                    "{\"fieldCode\":\"frecuencia\",\"fieldName\":\"Frecuencia\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Semanal\",\"Quincenal\",\"Mensual\"],\"displayOrder\":6}," +
                    "{\"fieldCode\":\"fecha_primera_cuota\",\"fieldName\":\"Primera Cuota\",\"fieldType\":\"date\",\"isRequired\":true,\"displayOrder\":7}," +
                    "{\"fieldCode\":\"responsable\",\"fieldName\":\"Responsable Autorización\",\"fieldType\":\"text\",\"isRequired\":true,\"displayOrder\":8}" +
                "]";

            case "EA_ENF": // Excepción por Enfermedad
                return "[" +
                    "{\"fieldCode\":\"tipo_enfermedad\",\"fieldName\":\"Tipo de Enfermedad\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Hospitalización\",\"Enfermedad Crónica\",\"Accidente\",\"Cirugía\",\"Otro\"],\"displayOrder\":1}," +
                    "{\"fieldCode\":\"persona_afectada\",\"fieldName\":\"Persona Afectada\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Cliente\",\"Familiar Directo\",\"Cónyuge\",\"Hijo/a\"],\"displayOrder\":2}," +
                    "{\"fieldCode\":\"fecha_inicio\",\"fieldName\":\"Fecha Inicio\",\"fieldType\":\"date\",\"isRequired\":true,\"displayOrder\":3}," +
                    "{\"fieldCode\":\"fecha_revision\",\"fieldName\":\"Fecha Revisión\",\"fieldType\":\"date\",\"isRequired\":true,\"displayOrder\":4}" +
                "]";

            case "EA_DES": // Excepción por Desempleo
                return "[" +
                    "{\"fieldCode\":\"fecha_cese\",\"fieldName\":\"Fecha de Cese\",\"fieldType\":\"date\",\"isRequired\":true,\"displayOrder\":1}," +
                    "{\"fieldCode\":\"motivo_cese\",\"fieldName\":\"Motivo\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Despido\",\"Renuncia\",\"Fin de Contrato\",\"Cierre Empresa\"],\"displayOrder\":2}," +
                    "{\"fieldCode\":\"busca_empleo\",\"fieldName\":\"¿Busca Empleo?\",\"fieldType\":\"checkbox\",\"isRequired\":false,\"defaultValue\":true,\"displayOrder\":3}," +
                    "{\"fieldCode\":\"fecha_revision\",\"fieldName\":\"Fecha Revisión\",\"fieldType\":\"date\",\"isRequired\":true,\"displayOrder\":4}" +
                "]";

            case "CD_SMT": // Cliente solicita más tiempo
                return "[" +
                    "{\"fieldCode\":\"motivo_solicitud\",\"fieldName\":\"Motivo de la Solicitud\",\"fieldType\":\"textarea\",\"isRequired\":true,\"displayOrder\":1}," +
                    "{\"fieldCode\":\"tiempo_adicional\",\"fieldName\":\"Tiempo Adicional\",\"fieldType\":\"text\",\"isRequired\":true,\"description\":\"Ej: 15 días, 1 mes\",\"displayOrder\":2}," +
                    "{\"fieldCode\":\"nueva_fecha\",\"fieldName\":\"Nueva Fecha Compromiso\",\"fieldType\":\"date\",\"isRequired\":true,\"displayOrder\":3}," +
                    "{\"fieldCode\":\"monto_comprometido\",\"fieldName\":\"Monto Comprometido\",\"fieldType\":\"currency\",\"isRequired\":false,\"displayOrder\":4}" +
                "]";

            case "AD_NT": // Nuevo Teléfono
                return "[" +
                    "{\"fieldCode\":\"nuevo_telefono\",\"fieldName\":\"Nuevo Teléfono\",\"fieldType\":\"phone\",\"isRequired\":true,\"displayOrder\":1}," +
                    "{\"fieldCode\":\"tipo_telefono\",\"fieldName\":\"Tipo\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Celular Personal\",\"Celular Trabajo\",\"Casa\",\"Referencia\"],\"displayOrder\":2}," +
                    "{\"fieldCode\":\"prioridad\",\"fieldName\":\"Prioridad\",\"fieldType\":\"select\",\"isRequired\":true,\"options\":[\"Principal\",\"Secundario\",\"Alternativo\"],\"displayOrder\":3}" +
                "]";

            case "AD_ND": // Nueva Dirección
                return "[" +
                    "{\"fieldCode\":\"nueva_direccion\",\"fieldName\":\"Nueva Dirección\",\"fieldType\":\"textarea\",\"isRequired\":true,\"displayOrder\":1}," +
                    "{\"fieldCode\":\"distrito\",\"fieldName\":\"Distrito\",\"fieldType\":\"text\",\"isRequired\":true,\"displayOrder\":2}," +
                    "{\"fieldCode\":\"provincia\",\"fieldName\":\"Provincia\",\"fieldType\":\"text\",\"isRequired\":true,\"displayOrder\":3}," +
                    "{\"fieldCode\":\"referencia\",\"fieldName\":\"Referencia\",\"fieldType\":\"text\",\"isRequired\":false,\"displayOrder\":4}" +
                "]";

            default:
                return null;
        }
    }

    /**
     * Asigna el tipo de clasificación correspondiente a una clasificación basándose en su código
     * Este método mapea códigos específicos (PT, PP, PPT, PF, CF) a sus tipos de clasificación
     */
    private void assignClassificationTypeIfApplicable(TypificationCatalog typification) {
        String code = typification.getCode();

        // Mapeo de códigos de tipificación a tipos de clasificación
        final String category;
        final String typeCode;

        switch (code) {
            case "PT" -> { category = "PAGO"; typeCode = "TOTAL"; }
            case "PP" -> { category = "PAGO"; typeCode = "PARCIAL"; }
            case "PPT" -> { category = "PAGO"; typeCode = "TOTAL"; } // Pago por tercero también es total
            case "PF" -> { category = "CRONOGRAMA"; typeCode = "FINANCIERA"; }
            case "CF" -> { category = "CRONOGRAMA"; typeCode = "FINANCIERA"; }
            default -> { return; } // Si no es un código conocido, salir
        }

        typificationCategoryRepository.findByCategoryAndCode(category, typeCode)
            .ifPresent(type -> {
                typification.setTypificationCategory(type);
                logger.info("    → Tipo asignado: {} - {}", category, typeCode);
            });
    }
}
