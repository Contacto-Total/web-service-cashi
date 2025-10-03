package com.cashi.systemconfiguration.interfaces.rest.resources;

public record ContactClassificationResource(
        Long id,
        String code,
        String label,
        Boolean isSuccessful
) {
}
