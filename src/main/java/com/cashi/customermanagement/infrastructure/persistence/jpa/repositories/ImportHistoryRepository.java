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

    // New methods for hash-based duplicate detection
    Optional<ImportHistory> findByFileHashAndStatus(String fileHash, String status);

    boolean existsByFileHashAndStatus(String fileHash, String status);

    // Duplicate detection by file name instead of hash
    // Allows same content with different file names to be processed
    boolean existsByFileNameAndStatus(String fileName, String status);

    // Duplicate detection by BOTH file name AND hash
    // Only reject if BOTH name and hash match (complete duplicate)
    // Allow if name is different even with same hash (different day, same content)
    boolean existsByFileNameAndFileHashAndStatus(String fileName, String fileHash, String status);
}
