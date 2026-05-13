package com.cashi.osiptelvalidation.domain.model.valueobjects;

/**
 * Tipo de documento aceptado por el portal Osiptel.
 * El value-text del select #IdTipoDoc se mapea desde el código del portal:
 *  DNI=1, CE=2, PASAPORTE=3, RUC=4 (confirmar con el HTML real).
 */
public enum DocumentType {
    DNI, CE, PASAPORTE, RUC
}
