package com.cashi.shared.infrastructure.persistence.jpa.repositories;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubPortfolioRepository extends JpaRepository<SubPortfolio, Integer> {

    /**
     * Encuentra todas las subcarteras de un portfolio
     */
    List<SubPortfolio> findByPortfolio(Portfolio portfolio);

    /**
     * Encuentra todas las subcarteras de un tenant (a través del portfolio)
     */
    List<SubPortfolio> findByPortfolio_Tenant(Tenant tenant);

    /**
     * Verifica si existe una subcartera con el código dado para un portfolio
     */
    boolean existsByPortfolioAndSubPortfolioCode(Portfolio portfolio, String subPortfolioCode);

    /**
     * Busca una subcartera por portfolio y código
     */
    Optional<SubPortfolio> findByPortfolioAndSubPortfolioCode(Portfolio portfolio, String subPortfolioCode);

    /**
     * Busca una subcartera por ID con su portfolio cargado (JOIN FETCH)
     */
    @Query("SELECT sp FROM SubPortfolio sp JOIN FETCH sp.portfolio WHERE sp.id = :id")
    Optional<SubPortfolio> findByIdWithPortfolio(@Param("id") Integer id);

    /**
     * Obtiene todas las subcarteras con su portfolio cargado (JOIN FETCH)
     */
    @Query("SELECT sp FROM SubPortfolio sp JOIN FETCH sp.portfolio")
    List<SubPortfolio> findAllWithPortfolio();

    /**
     * Cuenta cuántas subcarteras tiene un portfolio
     */
    long countByPortfolio(Portfolio portfolio);
}
