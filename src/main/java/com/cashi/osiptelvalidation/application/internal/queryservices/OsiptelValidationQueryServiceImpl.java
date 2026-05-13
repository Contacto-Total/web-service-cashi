package com.cashi.osiptelvalidation.application.internal.queryservices;

import com.cashi.osiptelvalidation.domain.model.aggregates.OsiptelValidation;
import com.cashi.osiptelvalidation.domain.model.queries.GetValidationByPhoneQuery;
import com.cashi.osiptelvalidation.domain.model.queries.GetValidationMetricsByPortfolioQuery;
import com.cashi.osiptelvalidation.domain.model.valueobjects.OperatorCode;
import com.cashi.osiptelvalidation.domain.model.valueobjects.ValidationStatus;
import com.cashi.osiptelvalidation.domain.services.OsiptelValidationQueryService;
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

    private final OsiptelValidationRepository repository;

    public OsiptelValidationQueryServiceImpl(OsiptelValidationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<OsiptelValidation> findLatestByPhone(GetValidationByPhoneQuery query) {
        return repository.findLatestOne(query.phone());
    }

    @Override
    public Map<String, Object> getMetrics(GetValidationMetricsByPortfolioQuery q) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Conteos por status
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ValidationStatus s : ValidationStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        for (Object[] row : repository.countByStatus(q.subPortfolioId(), q.from(), q.to())) {
            ValidationStatus s = (ValidationStatus) row[0];
            Long count = (Long) row[1];
            byStatus.put(s.name(), count);
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        result.put("total", total);
        result.put("byStatus", byStatus);

        // dni_match: true / false
        long dniMatchTrue = 0;
        long dniMatchFalse = 0;
        for (Object[] row : repository.countDniMatch(q.subPortfolioId(), q.from(), q.to())) {
            Boolean match = (Boolean) row[0];
            Long count = (Long) row[1];
            if (Boolean.TRUE.equals(match)) {
                dniMatchTrue = count;
            } else if (Boolean.FALSE.equals(match)) {
                dniMatchFalse = count;
            }
        }
        result.put("dniMatchTrue", dniMatchTrue);
        result.put("dniMatchFalse", dniMatchFalse);
        long matchEvaluated = dniMatchTrue + dniMatchFalse;
        result.put("dniMatchRate", matchEvaluated == 0 ? 0.0 : (double) dniMatchTrue / matchEvaluated);

        // Conteo por operador
        Map<String, Long> byOperator = new HashMap<>();
        for (Object[] row : repository.countByOperator(q.subPortfolioId(), q.from(), q.to())) {
            OperatorCode op = (OperatorCode) row[0];
            Long count = (Long) row[1];
            byOperator.put(op.name(), count);
        }
        result.put("byOperator", byOperator);

        return result;
    }
}
