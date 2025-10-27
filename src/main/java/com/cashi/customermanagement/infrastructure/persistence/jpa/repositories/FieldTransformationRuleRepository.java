package com.cashi.customermanagement.infrastructure.persistence.jpa.repositories;

import com.cashi.customermanagement.domain.model.entities.FieldTransformationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldTransformationRuleRepository extends JpaRepository<FieldTransformationRule, Long> {

    /**
     * Encuentra todas las reglas activas para un tenant y campo destino específico
     * Ordenadas por prioridad (rule_order ascendente)
     */
    List<FieldTransformationRule> findByTenantIdAndTargetFieldAndIsActiveTrueOrderByRuleOrderAsc(
            Long tenantId, String targetField);

    /**
     * Encuentra todas las reglas activas para un tenant
     */
    List<FieldTransformationRule> findByTenantIdAndIsActiveTrueOrderByRuleOrderAsc(Long tenantId);

    /**
     * Encuentra todas las reglas para un tenant (activas e inactivas)
     */
    List<FieldTransformationRule> findByTenantIdOrderByRuleOrderAsc(Long tenantId);

    /**
     * Encuentra todas las reglas activas para un tenant y subcartera específica
     * Ordenadas por prioridad (rule_order ascendente)
     */
    List<FieldTransformationRule> findByTenantIdAndSubPortfolioIdAndIsActiveTrueOrderByRuleOrderAsc(
            Long tenantId, Integer subPortfolioId);
}
