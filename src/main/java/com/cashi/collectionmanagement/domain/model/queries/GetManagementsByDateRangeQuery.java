package com.cashi.collectionmanagement.domain.model.queries;

import java.time.LocalDateTime;

public record GetManagementsByDateRangeQuery(
    LocalDateTime startDate,
    LocalDateTime endDate
) {
}
