package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import com.cashi.systemconfiguration.domain.model.entities.ConfigurationVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigurationVersionRepository extends JpaRepository<ConfigurationVersion, Long> {

    Optional<ConfigurationVersion> findByTenantAndPortfolioAndVersionNumber(
        Tenant tenant, Portfolio portfolio, Integer versionNumber
    );

    List<ConfigurationVersion> findByTenantAndPortfolioOrderByVersionNumberDesc(
        Tenant tenant, Portfolio portfolio
    );

    @Query("SELECT cv FROM ConfigurationVersion cv " +
           "WHERE cv.tenant = :tenant AND cv.portfolio = :portfolio AND cv.isActive = true")
    Optional<ConfigurationVersion> findActiveVersion(
        @Param("tenant") Tenant tenant, @Param("portfolio") Portfolio portfolio
    );

    @Query("SELECT MAX(cv.versionNumber) FROM ConfigurationVersion cv " +
           "WHERE cv.tenant = :tenant AND cv.portfolio = :portfolio")
    Optional<Integer> findMaxVersionNumber(
        @Param("tenant") Tenant tenant, @Param("portfolio") Portfolio portfolio
    );

    @Query("SELECT COUNT(cv) FROM ConfigurationVersion cv " +
           "WHERE cv.tenant = :tenant AND cv.portfolio = :portfolio")
    long countByTenantAndPortfolio(@Param("tenant") Tenant tenant, @Param("portfolio") Portfolio portfolio);
}
