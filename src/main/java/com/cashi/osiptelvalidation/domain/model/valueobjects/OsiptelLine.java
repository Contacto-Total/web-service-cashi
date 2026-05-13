package com.cashi.osiptelvalidation.domain.model.valueobjects;

/**
 * Una línea reportada por el portal Osiptel para un documento.
 * El número viene parcialmente enmascarado (5 dígitos visibles + 4 con '*'),
 * por eso se persiste solo el prefijo de 5 dígitos.
 */
public record OsiptelLine(
        String phonePrefix,    // 5 dígitos visibles, p.ej. "97851"
        OperatorCode operator,
        String modality        // CONTROL, POSTPAGO, etc. - nullable si el portal no lo trae
) {
    public OsiptelLine {
        if (phonePrefix == null || !phonePrefix.matches("\\d{4,6}")) {
            throw new IllegalArgumentException("phonePrefix inválido: " + phonePrefix);
        }
        if (operator == null) {
            throw new IllegalArgumentException("operator es obligatorio");
        }
    }
}
