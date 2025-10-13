package com.cashi.systemconfiguration.application.internal.queryservices;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.PortfolioRepository;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.TenantRepository;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationCatalog;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationConfigHistory;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationFieldMapping;
import com.cashi.systemconfiguration.domain.model.entities.ConfigurationVersion;
import com.cashi.systemconfiguration.domain.model.entities.TenantClassificationConfig;
import com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClassificationQueryServiceImpl {

    private final ClassificationCatalogRepository classificationCatalogRepository;
    private final TenantClassificationConfigRepository tenantClassificationConfigRepository;
    private final ClassificationDependencyRepository classificationDependencyRepository;
    private final ClassificationFieldMappingRepository classificationFieldMappingRepository;
    private final ClassificationConfigHistoryRepository historyRepository;
    private final ConfigurationVersionRepository versionRepository;
    private final TenantRepository tenantRepository;
    private final PortfolioRepository portfolioRepository;

    public ClassificationQueryServiceImpl(
            ClassificationCatalogRepository classificationCatalogRepository,
            TenantClassificationConfigRepository tenantClassificationConfigRepository,
            ClassificationDependencyRepository classificationDependencyRepository,
            ClassificationFieldMappingRepository classificationFieldMappingRepository,
            ClassificationConfigHistoryRepository historyRepository,
            ConfigurationVersionRepository versionRepository,
            TenantRepository tenantRepository,
            PortfolioRepository portfolioRepository) {
        this.classificationCatalogRepository = classificationCatalogRepository;
        this.tenantClassificationConfigRepository = tenantClassificationConfigRepository;
        this.classificationDependencyRepository = classificationDependencyRepository;
        this.classificationFieldMappingRepository = classificationFieldMappingRepository;
        this.historyRepository = historyRepository;
        this.versionRepository = versionRepository;
        this.tenantRepository = tenantRepository;
        this.portfolioRepository = portfolioRepository;
    }

    public List<ClassificationCatalog> getAllActiveClassifications() {
        return classificationCatalogRepository.findAllActive();
    }

    public List<ClassificationCatalog> getClassificationsByType(ClassificationCatalog.ClassificationType type) {
        return classificationCatalogRepository.findActiveByType(type);
    }

    public List<ClassificationCatalog> getRootClassificationsByType(ClassificationCatalog.ClassificationType type) {
        return classificationCatalogRepository.findRootByType(type);
    }

    public List<ClassificationCatalog> getChildClassifications(Long parentId) {
        return classificationCatalogRepository.findByParentId(parentId);
    }

    public List<ClassificationCatalog> getClassificationsByLevel(Integer level, ClassificationCatalog.ClassificationType type) {
        return classificationCatalogRepository.findByLevelAndType(level, type);
    }

    public Optional<ClassificationCatalog> getClassificationById(Long id) {
        return classificationCatalogRepository.findById(id);
    }

    public Optional<ClassificationCatalog> getClassificationByCode(String code) {
        return classificationCatalogRepository.findByCode(code);
    }

    public List<TenantClassificationConfig> getEnabledClassifications(Long tenantId, Long portfolioId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;

        return tenantClassificationConfigRepository.findEnabledByTenantAndPortfolio(tenant, portfolio);
    }

    public List<TenantClassificationConfig> getEnabledClassificationsByType(
            Long tenantId, Long portfolioId, ClassificationCatalog.ClassificationType type) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;

        return tenantClassificationConfigRepository.findEnabledByTenantPortfolioAndType(tenant, portfolio, type);
    }

    public List<TenantClassificationConfig> getEnabledClassificationsByLevel(
            Long tenantId, Long portfolioId, Integer level) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;

        return tenantClassificationConfigRepository.findEnabledByTenantPortfolioAndLevel(tenant, portfolio, level);
    }

    public List<TenantClassificationConfig> getChildClassificationsByParent(
            Long tenantId, Long portfolioId, Long parentId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;

        return tenantClassificationConfigRepository.findEnabledChildrenByParent(tenant, portfolio, parentId);
    }

    public long countEnabledClassifications(Long tenantId, Long portfolioId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;

        return tenantClassificationConfigRepository.countEnabledByTenantAndPortfolio(tenant, portfolio);
    }

    public Page<ClassificationConfigHistory> getChangeHistory(Long tenantId, Long portfolioId, Pageable pageable) {
        if (portfolioId != null) {
            return historyRepository.findByTenantIdAndPortfolioIdOrderByCreatedAtDesc(tenantId, portfolioId, pageable);
        }
        return historyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    public List<ClassificationConfigHistory> getEntityHistory(
            ClassificationConfigHistory.EntityType entityType, Long entityId) {
        return historyRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    public List<ConfigurationVersion> getVersionHistory(Long tenantId, Long portfolioId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;

        return versionRepository.findByTenantAndPortfolioOrderByVersionNumberDesc(tenant, portfolio);
    }

    public Optional<ConfigurationVersion> getActiveVersion(Long tenantId, Long portfolioId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;

        return versionRepository.findActiveVersion(tenant, portfolio);
    }

    /**
     * Obtiene todos los campos configurados para una clasificación específica
     * Solo las clasificaciones "hoja" (sin hijos) deberían tener campos asociados
     */
    public List<ClassificationFieldMapping> getClassificationFields(
            Long tenantId, Long portfolioId, Long classificationId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;

        return classificationFieldMappingRepository.findByTenantPortfolioAndClassification(
            tenant, portfolio, classificationId);
    }

    /**
     * Obtiene solo los campos visibles para una clasificación
     */
    public List<ClassificationFieldMapping> getVisibleClassificationFields(
            Long tenantId, Long portfolioId, Long classificationId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Portfolio portfolio = portfolioId != null ? portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found")) : null;

        return classificationFieldMappingRepository.findVisibleFieldsByClassification(
            tenant, portfolio, classificationId);
    }

    /**
     * Verifica si una clasificación es "hoja" (no tiene hijos habilitados)
     * Solo las clasificaciones hoja deberían mostrar formularios
     */
    public boolean isLeafClassification(Long tenantId, Long portfolioId, Long classificationId) {
        List<TenantClassificationConfig> children = getChildClassificationsByParent(
            tenantId, portfolioId, classificationId);
        return children.isEmpty();
    }
}
