package com.cashi.osiptelvalidation.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request: POST /api/v1/osiptel/batches
 *
 * El DNI viaja en el body y se usa SOLO para calcular dni_hash; nunca se persiste.
 */
public record EnqueueBatchRequest(@Valid @NotEmpty List<PhoneItem> phones) {

    public record PhoneItem(
            @NotNull String phone,
            @NotNull String dni,
            Long subPortfolioId,
            Long contactMethodId,
            @NotNull Long tenantId
    ) {}
}
