package com.cashi.systemconfiguration.interfaces.rest.resources;

public record DisplayOrderUpdateCommand(
    Long id,
    Integer displayOrder
) {
}
