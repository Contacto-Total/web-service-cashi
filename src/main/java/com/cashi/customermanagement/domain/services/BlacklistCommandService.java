package com.cashi.customermanagement.domain.services;

import com.cashi.customermanagement.domain.model.aggregates.Blacklist;

public interface BlacklistCommandService {
    Blacklist createBlacklist(Blacklist blacklist);
    Blacklist updateBlacklist(Long id, Blacklist blacklist);
    void deleteBlacklist(Long id);
}
