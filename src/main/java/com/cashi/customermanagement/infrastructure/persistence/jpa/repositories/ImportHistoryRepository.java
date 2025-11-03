package com.cashi.customermanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.customermanagement.domain.model.aggregates.ImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {

    List<ImportHistory> findBySubPortfolioIdOrderByProcessedAtDesc(Long subPortfolioId);

    List<ImportHistory> findByProcessedAtAfterOrderByProcessedAtDesc(LocalDateTime after);

    Optional<ImportHistory> findByFilePathAndStatus(String filePath, String status);

    boolean existsByFilePathAndStatus(String filePath, String status);
}
