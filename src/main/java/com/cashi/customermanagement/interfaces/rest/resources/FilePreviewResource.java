package com.cashi.customermanagement.interfaces.rest.resources;

public record FilePreviewResource(
        String name,
        String size,
        String modifiedDate,
        Boolean processed
) {
}
