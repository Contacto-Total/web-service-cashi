package com.cashi.customermanagement.interfaces.rest.resources;

public record ContactInfoResource(
        String primaryPhone,
        String alternativePhone,
        String workPhone,
        String email,
        String address
) {
}
