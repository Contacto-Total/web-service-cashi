package com.cashi.osiptelvalidation.domain.model.queries;

/**
 * Query: última validación conocida para un número.
 * Devuelve la fila más reciente por finished_at (o por id si no hay finished_at).
 */
public record GetValidationByPhoneQuery(String phone) {}
