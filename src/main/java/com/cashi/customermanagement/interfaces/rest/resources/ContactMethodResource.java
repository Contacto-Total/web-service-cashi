package com.cashi.customermanagement.interfaces.rest.resources;

import java.time.LocalDate;

public record ContactMethodResource(
        Long id,
        String contactType,
        String subtype,
        String value,
        String label,
        LocalDate importDate,
        String status,
        // V17+: campos del modelo NO-ortogonal
        String estadoOsiptel,
        String estadoWhatsapp
) {
}
