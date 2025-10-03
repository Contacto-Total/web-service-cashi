package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.FieldDefinition;
import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationCatalog;
import com.cashi.systemconfiguration.domain.model.entities.ClassificationFieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassificationFieldMappingRepository extends JpaRepository<ClassificationFieldMapping, Long> {

    Optional<ClassificationFieldMapping> findByTenantAndPortfolioAndClassificationAndFieldDefinition(
        Tenant tenant, Portfolio portfolio, ClassificationCatalog classification, FieldDefinition fieldDefinition
    );

    @Query("SELECT cfm FROM ClassificationFieldMapping cfm " +
           "WHERE cfm.tenant = :tenant AND cfm.portfolio = :portfolio " +
           "AND cfm.classification.id = :classificationId " +
           "ORDER BY cfm.displayOrder")
    List<ClassificationFieldMapping> findByTenantPortfolioAndClassification(
        @Param("tenant") Tenant tenant,
        @Param("portfolio") Portfolio portfolio,
        @Param("classificationId") Long classificationId
    );

    @Query("SELECT cfm FROM ClassificationFieldMapping cfm " +
           "WHERE cfm.tenant = :tenant AND cfm.portfolio = :portfolio " +
           "AND cfm.classification.id = :classificationId " +
           "AND cfm.isRequired = true " +
           "ORDER BY cfm.displayOrder")
    List<ClassificationFieldMapping> findRequiredFieldsByClassification(
        @Param("tenant") Tenant tenant,
        @Param("portfolio") Portfolio portfolio,
        @Param("classificationId") Long classificationId
    );

    @Query("SELECT cfm FROM ClassificationFieldMapping cfm " +
           "WHERE cfm.tenant = :tenant AND cfm.portfolio = :portfolio " +
           "AND cfm.classification.id = :classificationId " +
           "AND cfm.isVisible = true " +
           "ORDER BY cfm.displayOrder")
    List<ClassificationFieldMapping> findVisibleFieldsByClassification(
        @Param("tenant") Tenant tenant,
        @Param("portfolio") Portfolio portfolio,
        @Param("classificationId") Long classificationId
    );
}
