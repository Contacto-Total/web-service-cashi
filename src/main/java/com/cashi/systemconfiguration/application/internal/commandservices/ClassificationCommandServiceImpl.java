package com.cashi.systemconfiguration.application.internal.commandservices;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.TenantRepository;
import com.cashi.systemconfiguration.domain.model.entities.*;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassificationCommandServiceImpl {

    private final ClassificationCatalogRepository classificationCatalogRepository;
    private final TenantClassificationConfigRepository tenantClassificationConfigRepository;
    private final ClassificationDependencyRepository classificationDependencyRepository;
    private final ClassificationFieldMappingRepository classificationFieldMappingRepository;
    private final ClassificationConfigHistoryRepository historyRepository;
    private final ConfigurationVersionRepository versionRepository;
    private final TenantRepository tenantRepository;
    private final PortfolioRepository portfolioRepository;
    private final ObjectMapper objectMapper;

    public ClassificationCommandServiceImpl(
            ClassificationCatalogRepository classificationCatalogRepository,
            TenantClassificationConfigRepository tenantClassificationConfigRepository,
            ClassificationDependencyRepository classificationDependencyRepository,
            ClassificationFieldMappingRepository classificationFieldMappingRepository,
            ClassificationConfigHistoryRepository historyRepository,
            ConfigurationVersionRepository versionRepository,
            TenantRepository tenantRepository,
            PortfolioRepository portfolioRepository,
            ObjectMapper objectMapper) {
        this.classificationCatalogRepository = classificationCatalogRepository;
        this.tenantClassificationConfigRepository = tenantClassificationConfigRepository;
        this.classificationDependencyRepository = classificationDependencyRepository;
        this.classificationFieldMappingRepository = classificationFieldMappingRepository;
        this.historyRepository = historyRepository;
        this.versionRepository = versionRepository;
        this.tenantRepository = tenantRepository;
        this.portfolioRepository = portfolioRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ClassificationCatalog createClassification(String code, String name,
                                                     ClassificationCatalog.ClassificationType type,
                                                     Long parentId, String createdBy) {
        if (classificationCatalogRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Classification code already exists: " + code);
        }

        ClassificationCatalog parent = null;
        if (parentId != null) {
            parent = classificationCatalogRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent classification not found"));
        }

        ClassificationCatalog classification = new ClassificationCatalog(code, name, type, parent, null);
        ClassificationCatalog saved = classificationCatalogRepository.save(classification);

        // Audit
        logChange(ClassificationConfigHistory.EntityType.CLASSIFICATION, saved.getId(),
                 ClassificationConfigHistory.ChangeType.CREATE, createdBy, null, saved);

        return saved;
    }

    @Transactional
    public ClassificationCatalog updateClassification(Long id, String name, String description,
                                                     String iconName, String colorHex, String changedBy) {
        ClassificationCatalog classification = classificationCatalogRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Classification not found"));

        ClassificationCatalog previous = cloneClassification(classification);

        classification.setName(name);
        classification.setDescription(description);
        classification.setIconName(iconName);
        classification.setColorHex(colorHex);

        ClassificationCatalog saved = classificationCatalogRepository.save(classification);

        // Audit
        logChange(ClassificationConfigHistory.EntityType.CLASSIFICATION, saved.getId(),
                 ClassificationConfigHistory.ChangeType.UPDATE, changedBy, previous, saved);

        return saved;
    }

    @Transactional
    public void deleteClassification(Long id, String deletedBy) {
        ClassificationCatalog classification = classificationCatalogRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Classification not found"));

        if (classification.getIsSystem()) {
            throw new IllegalArgumentException("Cannot delete system classification");
        }

        if (classificationCatalogRepository.hasChildren(id)) {
            throw new IllegalArgumentException("Cannot delete classification with children");
        }

        ClassificationCatalog previous = cloneClassification(classification);
        classification.softDelete();
        classificationCatalogRepository.save(classification);

        // Audit
        logChange(ClassificationConfigHistory.EntityType.CLASSIFICATION, classification.getId(),
                 ClassificationConfigHistory.ChangeType.DELETE, deletedBy, previous, classification);
    }

    @Transactional
    public TenantClassificationConfig enableClassification(Long tenantId, Long portfolioId,
                                                          Long classificationId, String enabledBy) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;
        ClassificationCatalog classification = classificationCatalogRepository.findById(classificationId)
            .orElseThrow(() -> new IllegalArgumentException("Classification not found"));

        TenantClassificationConfig config = tenantClassificationConfigRepository
            .findByTenantAndPortfolioAndClassification(tenant, portfolio, classification)
            .orElse(new TenantClassificationConfig(tenant, portfolio, classification, true));

        TenantClassificationConfig previous = cloneConfig(config);
        config.setIsEnabled(true);
        config.setCreatedBy(enabledBy);

        TenantClassificationConfig saved = tenantClassificationConfigRepository.save(config);

        // Audit
        logChange(ClassificationConfigHistory.EntityType.CONFIG, saved.getId(),
                 ClassificationConfigHistory.ChangeType.ENABLE, enabledBy, previous, saved);

        return saved;
    }

    @Transactional
    public TenantClassificationConfig disableClassification(Long tenantId, Long portfolioId,
                                                           Long classificationId, String disabledBy) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;
        ClassificationCatalog classification = classificationCatalogRepository.findById(classificationId)
            .orElseThrow(() -> new IllegalArgumentException("Classification not found"));

        TenantClassificationConfig config = tenantClassificationConfigRepository
            .findByTenantAndPortfolioAndClassification(tenant, portfolio, classification)
            .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));

        TenantClassificationConfig previous = cloneConfig(config);
        config.setIsEnabled(false);

        TenantClassificationConfig saved = tenantClassificationConfigRepository.save(config);

        // Audit
        logChange(ClassificationConfigHistory.EntityType.CONFIG, saved.getId(),
                 ClassificationConfigHistory.ChangeType.DISABLE, disabledBy, previous, saved);

        return saved;
    }

    @Transactional
    public TenantClassificationConfig updateClassificationConfig(Long tenantId, Long portfolioId,
                                                                Long classificationId,
                                                                String customName, Integer customOrder,
                                                                String customIcon, String customColor,
                                                                Boolean requiresComment,
                                                                String changedBy) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;
        ClassificationCatalog classification = classificationCatalogRepository.findById(classificationId)
            .orElseThrow(() -> new IllegalArgumentException("Classification not found"));

        TenantClassificationConfig config = tenantClassificationConfigRepository
            .findByTenantAndPortfolioAndClassification(tenant, portfolio, classification)
            .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));

        TenantClassificationConfig previous = cloneConfig(config);

        config.setCustomName(customName);
        config.setCustomOrder(customOrder);
        config.setCustomIcon(customIcon);
        config.setCustomColor(customColor);
        config.setRequiresComment(requiresComment != null ? requiresComment : false);

        TenantClassificationConfig saved = tenantClassificationConfigRepository.save(config);

        // Audit
        logChange(ClassificationConfigHistory.EntityType.CONFIG, saved.getId(),
                 ClassificationConfigHistory.ChangeType.UPDATE, changedBy, previous, saved);

        return saved;
    }

    @Transactional
    public ConfigurationVersion createSnapshot(Long tenantId, Long portfolioId,
                                              String versionName, String description,
                                              String createdBy) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;

        // Get current version number
        Integer nextVersion = versionRepository.findMaxVersionNumber(tenant, portfolio)
            .map(v -> v + 1)
            .orElse(1);

        // Snapshot all configurations
        List<TenantClassificationConfig> configs = tenantClassificationConfigRepository
            .findByTenantAndPortfolio(tenant, portfolio);

        String snapshotData;
        try {
            snapshotData = objectMapper.writeValueAsString(configs);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create snapshot", e);
        }

        ConfigurationVersion version = new ConfigurationVersion(
            tenant, portfolio, nextVersion, versionName, snapshotData, createdBy
        );
        version.setDescription(description);

        return versionRepository.save(version);
    }

    @Transactional
    public void activateVersion(Long versionId, String activatedBy) {
        ConfigurationVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new IllegalArgumentException("Version not found"));

        // Deactivate current active version
        versionRepository.findActiveVersion(version.getTenant(), version.getPortfolio())
            .ifPresent(ConfigurationVersion::deactivate);

        version.activate(activatedBy);
        versionRepository.save(version);

        // Audit
        logChange(ClassificationConfigHistory.EntityType.VERSION, version.getId(),
                 ClassificationConfigHistory.ChangeType.UPDATE, activatedBy, null, version);
    }

    private void logChange(ClassificationConfigHistory.EntityType entityType, Long entityId,
                          ClassificationConfigHistory.ChangeType changeType, String changedBy,
                          Object previousValue, Object newValue) {
        try {
            String previousJson = previousValue != null ? objectMapper.writeValueAsString(previousValue) : null;
            String newJson = newValue != null ? objectMapper.writeValueAsString(newValue) : null;

            ClassificationConfigHistory history = new ClassificationConfigHistory(
                entityType, entityId, changeType, changedBy, previousJson, newJson
            );
            historyRepository.save(history);
        } catch (Exception e) {
            // Log error but don't fail the transaction
            e.printStackTrace();
        }
    }

    private ClassificationCatalog cloneClassification(ClassificationCatalog original) {
        ClassificationCatalog clone = new ClassificationCatalog();
        clone.setCode(original.getCode());
        clone.setName(original.getName());
        clone.setDescription(original.getDescription());
        clone.setIconName(original.getIconName());
        clone.setColorHex(original.getColorHex());
        return clone;
    }

    private TenantClassificationConfig cloneConfig(TenantClassificationConfig original) {
        TenantClassificationConfig clone = new TenantClassificationConfig();
        clone.setIsEnabled(original.getIsEnabled());
        clone.setCustomName(original.getCustomName());
        clone.setCustomOrder(original.getCustomOrder());
        clone.setCustomIcon(original.getCustomIcon());
        clone.setCustomColor(original.getCustomColor());
        return clone;
    }
}
