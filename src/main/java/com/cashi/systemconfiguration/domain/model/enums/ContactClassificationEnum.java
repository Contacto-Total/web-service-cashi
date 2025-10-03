package com.cashi.systemconfiguration.domain.model.enums;

public enum ContactClassificationEnum {
    CPC("CPC", "Contacto con Cliente", true),
    CTT("CTT", "Contacto con Tercero", true),
    NCL("NCL", "No Contesta", false),
    BZN("BZN", "Buzón de Voz", false),
    OCP("OCP", "Ocupado", false),
    FTN("FTN", "Fuera de Tono", false),
    NEQ("NEQ", "Número Equivocado", false),
    TEL("TEL", "Teléfono Inválido", false),
    CLG("CLG", "Cliente Colgó", false),
    RCH("RCH", "Rechazó Llamada", false);

    private final String code;
    private final String description;
    private final Boolean isSuccessful;

    ContactClassificationEnum(String code, String description, Boolean isSuccessful) {
        this.code = code;
        this.description = description;
        this.isSuccessful = isSuccessful;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getIsSuccessful() {
        return isSuccessful;
    }
}
