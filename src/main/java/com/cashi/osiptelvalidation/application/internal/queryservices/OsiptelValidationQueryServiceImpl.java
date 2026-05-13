package com.cashi.osiptelvalidation.application.internal.queryservices;

import com.cashi.osiptelvalidation.domain.model.aggregates.OsiptelValidation;
import com.cashi.osiptelvalidation.domain.model.entities.OsiptelPhoneMatch;
import com.cashi.osiptelvalidation.domain.model.queries.GetValidationByPhoneQuery;
import com.cashi.osiptelvalidation.domain.model.queries.GetValidationMetricsByPortfolioQuery;
import com.cashi.osiptelvalidation.domain.model.valueobjects.OperatorCode;
import com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus;
import com.cashi.osiptelvalidation.domain.services.OsiptelValidationQueryService;
import com.cashi.osiptelvalidation.infrastructure.persistence.jpa.repositories.OsiptelPhoneMatchRepository;
import com.cashi.osiptelvalidation.infrastructure.persistence.jpa.repositories.OsiptelValidationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OsiptelValidationQueryServiceImpl implements OsiptelValidationQueryService {

    private final OsiptelValidationRepository validationRepository;
    private final OsiptelPhoneMatchRepository phoneMatchRepository;

    public OsiptelValidationQueryServiceImpl(OsiptelValidationRepository validationRepository,
                                             OsiptelPhoneMatchRepository phoneMatchRepository) {
        this.validationRepository = validationRepository;
        this.phoneMatchRepository = phoneMatchRepository;
    }

    @Override
    public Optional<PhoneValidationView> findLatestByPhone(GetValidationByPhoneQuery query) {
        return phoneMatchRepository.findTopByPhoneOrderByCreatedAtDesc(query.phone())
                .map(this::toView);
    }

    private PhoneValidationView toView(OsiptelPhoneMatch match) {
        // Cargar la validación padre para status/cooldown/attempts
        OsiptelValidation parent = validationRepository.findById(match.getValidationId()).orElse(null);
        String status = parent != null ? parent.getStatus().name() : "OK";
        return new PhoneValidationView(
                match.getPhone(),
                status,
                match.getDniMatch(),
                match.getMatchedOperator(),
                match.getMatchedModality(),
                match.getCreatedAt(),
                parent != null ? parent.getCooldownUntil() : null,
                parent != null && parent.getAttempts() != null ? parent.getAttempts().intValue() : 0
        );
    }

    @Override
    public Map<String, Object> getMetrics(GetValidationMetricsByPortfolioQuery q) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Conteos por status de validacion
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ValidationStatus s : ValidationStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        for (Object[] row : validationRepository.countByStatus(q.subPortfolioId(), q.from(), q.to())) {
            ValidationStatus s = (ValidationStatus) row[0];
            Long count = (Long) row[1];
            byStatus.put(s.name(), count);
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        result.put("totalValidations", total);
        result.put("byStatus", byStatus);

        Long totalLines = validationRepository.totalLinesReturned(q.subPortfolioId(), q.from(), q.to());
        result.put("totalLinesReturned", totalLines == null ? 0L : totalLines);

        // Phone matches: hits vs misses (independiente de subportfolio porque los matches
        // se referencian a la validation; un join sería costoso; reportería global por rango)
        long matchTrue = 0;
        long matchFalse = 0;
        for (Object[] row : phoneMatchRepository.countByMatch(q.from(), q.to())) {
            Boolean m = (Boolean) row[0];
            Long count = (Long) row[1];
            if (Boolean.TRUE.equals(m)) matchTrue = count;
            else if (Boolean.FALSE.equals(m)) matchFalse = count;
        }
        result.put("phoneMatches", matchTrue);
        result.put("phoneMisses", matchFalse);
        long evaluated = matchTrue + matchFalse;
        result.put("phoneMatchRate", evaluated == 0 ? 0.0 : (double) matchTrue / evaluated);

        Map<String, Long> byOperator = new HashMap<>();
        for (Object[] row : phoneMatchRepository.countByOperator(q.from(), q.to())) {
            OperatorCode op = (OperatorCode) row[0];
            Long count = (Long) row[1];
            byOperator.put(op.name(), count);
        }
        result.put("byOperator", byOperator);

        return result;
    }
}
