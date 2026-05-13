package com.cashi.osiptelvalidation.infrastructure.persistence.jpa.repositories;

import com.cashi.osiptelvalidation.domain.model.entities.OsiptelValidationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OsiptelValidationAttemptRepository extends JpaRepository<OsiptelValidationAttempt, Long> {

    List<OsiptelValidationAttempt> findByValidationIdOrderByAttemptNoAsc(Long validationId);
}
