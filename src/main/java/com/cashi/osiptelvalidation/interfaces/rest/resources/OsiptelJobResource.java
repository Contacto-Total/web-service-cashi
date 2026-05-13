package com.cashi.osiptelvalidation.interfaces.rest.resources;

/**
 * Job que el Electron app reclama del backend.
 *
 * El DNI plaintext viaja en la respuesta del claim y NO se persiste localmente
 * en la app (privacidad). Solo existe en memoria mientras procesa el job.
 */
public record OsiptelJobResource(
        Long validationId,
        String dni,
        String dniType   // DNI | CE | PASAPORTE | RUC
) {}
