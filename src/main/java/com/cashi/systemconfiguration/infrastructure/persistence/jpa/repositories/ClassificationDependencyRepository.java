package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationCatalog;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassificationDependencyRepository extends JpaRepository<ClassificationDependency, Long> {

    List<ClassificationDependency> findByTenantAndPortfolio(Tenant tenant, Portfolio portfolio);

    @Query("SELECT cd FROM ClassificationDependency cd " +
           "WHERE cd.tenant = :tenant AND cd.portfolio = :portfolio " +
           "AND cd.parentClassification.id = :parentId " +
           "ORDER BY cd.displayOrder")
    List<ClassificationDependency> findByTenantPortfolioAndParent(
        @Param("tenant") Tenant tenant,
        @Param("portfolio") Portfolio portfolio,
        @Param("parentId") Long parentId
    );

    @Query("SELECT cd FROM ClassificationDependency cd " +
           "WHERE cd.tenant = :tenant AND cd.portfolio = :portfolio " +
           "AND cd.parentClassification.id = :parentId " +
           "AND cd.dependencyType = :type " +
           "ORDER BY cd.displayOrder")
    List<ClassificationDependency> findByTenantPortfolioParentAndType(
        @Param("tenant") Tenant tenant,
        @Param("portfolio") Portfolio portfolio,
        @Param("parentId") Long parentId,
        @Param("type") ClassificationDependency.DependencyType type
    );

    @Query("SELECT cd FROM ClassificationDependency cd " +
           "WHERE cd.tenant = :tenant AND cd.portfolio = :portfolio " +
           "AND cd.childClassification.id = :childId")
    List<ClassificationDependency> findByTenantPortfolioAndChild(
        @Param("tenant") Tenant tenant,
        @Param("portfolio") Portfolio portfolio,
        @Param("childId") Long childId
    );
}
