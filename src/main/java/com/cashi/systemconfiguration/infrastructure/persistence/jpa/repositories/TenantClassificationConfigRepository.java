package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationCatalog;
import com.cashi.systemconfiguration.domain.model.entities.TenantClassificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantClassificationConfigRepository extends JpaRepository<TenantClassificationConfig, Long> {

    Optional<TenantClassificationConfig> findByTenantAndPortfolioAndClassification(
        Tenant tenant, Portfolio portfolio, ClassificationCatalog classification
    );

    @Query("SELECT tcc FROM TenantClassificationConfig tcc " +
           "LEFT JOIN FETCH tcc.classification c " +
           "LEFT JOIN FETCH c.classificationTypeCatalog " +
           "WHERE tcc.tenant = :tenant " +
           "AND (:portfolio IS NULL OR tcc.portfolio = :portfolio OR tcc.portfolio IS NULL) " +
           "AND tcc.isEnabled = true " +
           "ORDER BY tcc.customOrder, c.displayOrder")
    List<TenantClassificationConfig> findEnabledByTenantAndPortfolio(
        @Param("tenant") Tenant tenant, @Param("portfolio") Portfolio portfolio
    );

    @Query("SELECT tcc FROM TenantClassificationConfig tcc " +
           "LEFT JOIN FETCH tcc.classification " +
           "WHERE tcc.tenant = :tenant " +
           "AND (:portfolio IS NULL OR tcc.portfolio = :portfolio OR tcc.portfolio IS NULL) " +
           "AND tcc.classification.classificationType = :type AND tcc.isEnabled = true " +
           "ORDER BY tcc.customOrder, tcc.classification.displayOrder")
    List<TenantClassificationConfig> findEnabledByTenantPortfolioAndType(
        @Param("tenant") Tenant tenant,
        @Param("portfolio") Portfolio portfolio,
        @Param("type") ClassificationCatalog.ClassificationType type
    );

    @Query("SELECT tcc FROM TenantClassificationConfig tcc " +
           "LEFT JOIN FETCH tcc.classification " +
           "WHERE tcc.tenant = :tenant " +
           "AND (:portfolio IS NULL OR tcc.portfolio = :portfolio OR tcc.portfolio IS NULL) " +
           "AND tcc.classification.hierarchyLevel = :level AND tcc.isEnabled = true " +
           "ORDER BY tcc.customOrder, tcc.classification.displayOrder")
    List<TenantClassificationConfig> findEnabledByTenantPortfolioAndLevel(
        @Param("tenant") Tenant tenant,
        @Param("portfolio") Portfolio portfolio,
        @Param("level") Integer level
    );

    @Query("SELECT tcc FROM TenantClassificationConfig tcc " +
           "LEFT JOIN FETCH tcc.classification " +
           "WHERE tcc.tenant = :tenant " +
           "AND (:portfolio IS NULL OR tcc.portfolio = :portfolio OR tcc.portfolio IS NULL) " +
           "AND tcc.classification.parentClassification.id = :parentId AND tcc.isEnabled = true " +
           "ORDER BY tcc.customOrder, tcc.classification.displayOrder")
    List<TenantClassificationConfig> findEnabledChildrenByParent(
        @Param("tenant") Tenant tenant,
        @Param("portfolio") Portfolio portfolio,
        @Param("parentId") Long parentId
    );

    List<TenantClassificationConfig> findByTenantAndPortfolio(Tenant tenant, Portfolio portfolio);

    List<TenantClassificationConfig> findByTenant(Tenant tenant);

    @Query("SELECT COUNT(tcc) FROM TenantClassificationConfig tcc " +
           "WHERE tcc.tenant = :tenant AND tcc.portfolio = :portfolio AND tcc.isEnabled = true")
    long countEnabledByTenantAndPortfolio(@Param("tenant") Tenant tenant, @Param("portfolio") Portfolio portfolio);
}
