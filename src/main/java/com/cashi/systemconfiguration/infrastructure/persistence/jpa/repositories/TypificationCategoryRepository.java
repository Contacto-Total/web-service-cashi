package com.cashi.systemconfiguration.infrastructure.persistence.jpa.repositories;

import com.cashi.systemconfiguration.domain.model.entities.TypificationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypificationCategoryRepository extends JpaRepository<TypificationCategory, Integer> {

    Optional<TypificationCategory> findByCategoryAndCode(String category, String code);

    List<TypificationCategory> findByCategory(String category);

    List<TypificationCategory> findByIsActiveTrue();

    List<TypificationCategory> findByCategoryAndIsActiveTrue(String category);
}
