package com.cashi.systemconfiguration.interfaces.rest.resources;

import com.cashi.shared.domain.model.valueobjects.LoadType;

import java.util.List;
import java.util.Map;

public record ImportDataResource(
        Integer subPortfolioId,
        LoadType loadType,
        List<Map<String, Object>> data
) {
}
