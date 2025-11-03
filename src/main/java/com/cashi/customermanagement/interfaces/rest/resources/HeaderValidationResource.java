package com.cashi.customermanagement.interfaces.rest.resources;

public record HeaderValidationResource(
        Boolean valid,
        String message
) {
}
