package com.cashi.systemconfiguration.domain.model.enums;

import lombok.Getter;

@Getter
public enum ManagementClassificationEnum {
    // Pagos y compromisos
    ACP("ACP", "Acepta Compromiso de Pago", true, false, true),
    PPR("PPR", "Promesa de Pago Realizada", true, false, true),
    PGR("PGR", "Pago Realizado", true, false, false),
    PGP("PGP", "Pago Parcial", true, false, false),

    // Convenios y acuerdos
    CNV("CNV", "Convenio de Pago", true, true, false),
    REF("REF", "Refinanciamiento", true, true, false),

    // Solicitudes
    SIC("SIC", "Solicita Información de Cuenta", false, false, false),
    SCR("SCR", "Solicita Cronograma de Pagos", false, false, false),
    SEP("SEP", "Solicita Estado de Cuenta", false, false, false),

    // Rechazos y problemas
    NPC("NPC", "No Puede/No tiene para Pagar", false, false, true),
    DIS("DIS", "Disputa de Deuda", false, false, true),
    DES("DES", "Desconoce la Deuda", false, false, false),

    // Reclamos
    RCL("RCL", "Presenta Reclamo", false, false, true),
    FRD("FRD", "Reporta Fraude", false, false, true),

    // Situaciones especiales
    AGR("AGR", "Cliente Agresivo", false, false, false),
    NBL("NBL", "No Blow (Cliente difícil)", false, false, false),
    LGL("LGL", "Remite a Legal", false, false, true);

    private final String code;
    private final String description;
    private final Boolean requiresPayment;
    private final Boolean requiresSchedule;
    private final Boolean requiresFollowUp;

    ManagementClassificationEnum(String code, String description, Boolean requiresPayment,
                                  Boolean requiresSchedule, Boolean requiresFollowUp) {
        this.code = code;
        this.description = description;
        this.requiresPayment = requiresPayment;
        this.requiresSchedule = requiresSchedule;
        this.requiresFollowUp = requiresFollowUp;
    }
}
