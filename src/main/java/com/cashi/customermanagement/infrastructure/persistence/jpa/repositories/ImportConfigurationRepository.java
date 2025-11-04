package com.cashi.customermanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.customermanagement.domain.model.aggregates.ImportConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImportConfigurationRepository extends JpaRepository<ImportConfiguration, Long> {

    Optional<ImportConfiguration> findByActiveTrue();

    Optional<ImportConfiguration> findFirstByOrderByIdDesc();
}
