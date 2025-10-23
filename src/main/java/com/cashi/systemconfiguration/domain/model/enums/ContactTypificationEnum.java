package com.cashi.systemconfiguration.domain.model.enums;

import lombok.Getter;

@Getter
public enum ContactTypificationEnum {
    CPC("CPC", "Contacto Personal con Cliente", true),
    CTT("CTT", "Contacto con Terceros", true),
    NCL("NCL", "No Contacto por Línea Ocupada", false),
    NCC("NCC", "No Contacto por Colgó", false),
    NCN("NCN", "No Contacto por Número Equivocado", false),
    NCB("NCB", "No Contacto por Buzón de Voz", false),
    NCA("NCA", "No Contacto por Apagado", false),
    BZN("BZN", "Buzón de Voz", false);

    private final String code;
    private final String description;
    private final Boolean isSuccessful;

    ContactTypificationEnum(String code, String description, Boolean isSuccessful) {
        this.code = code;
        this.description = description;
        this.isSuccessful = isSuccessful;
    }
}
