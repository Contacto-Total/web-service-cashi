package com.cashi.systemconfiguration.interfaces.rest.resources;

public record DisplayOrderUpdateCommand(
    Integer id,
    Integer displayOrder
) {
}
