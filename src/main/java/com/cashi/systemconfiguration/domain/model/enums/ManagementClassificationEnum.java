package com.cashi.systemconfiguration.domain.model.enums;

public enum ManagementClassificationEnum {
    // Pagos y compromisos
    ACP("ACP", "Acepta Compromiso de Pago", true, true),
    PGR("PGR", "Pago Realizado - Completo", true, false),
    PGP("PGP", "Pago Parcial Realizado", true, false),
    PGT("PGT", "Pago Total de Deuda", true, false),
    PPR("PPR", "Pago Programado", true, true),

    // Solicitudes especiales
    SRP("SRP", "Solicita Refinanciamiento", false, false),
    SQA("SQA", "Solicita Quita o Descuento", false, false),
    SCN("SCN", "Solicita Congelamiento", false, false),
    SPL("SPL", "Solicita Ampliación de Plazo", false, false),

    // Disputas y problemas
    DPD("DPD", "Disputa de Deuda", false, false),
    NRD("NRD", "No Reconoce Deuda", false, false),
    DIF("DIF", "Dificultad Financiera Temporal", false, false),
    DSE("DSE", "Desempleo", false, false),
    ENF("ENF", "Enfermedad/Incapacidad", false, false),
    FLC("FLC", "Fallecimiento del Titular", false, false),
    RCL("RCL", "Reclamo Registrado", false, false),
    FRD("FRD", "Reporte de Fraude", false, false),
    NCB("NCB", "Sin Capacidad de Pago", false, false),

    // Seguimiento
    VJE("VJE", "Viaje/Fuera del País", false, true),
    SLL("SLL", "Solicita Llamar Después", false, true),
    NIN("NIN", "No está Interesado", false, false),
    AGR("AGR", "Cliente Agresivo/Hostil", false, false),
    NBL("NBL", "No Desea Ser Contactado", false, false),
    LGL("LGL", "Amenaza Legal", false, false),

    // Convenios y cronogramas
    CNV("CNV", "Acepta Convenio de Pago", false, true),
    CRO("CRO", "Solicita Cronograma de Pagos", false, true),
    CPP("CPP", "Convenio con Pago Parcial Inicial", true, true),
    CTT_CONV("CTT", "Convenio a Plazo Total", false, true),
    CCG("CCG", "Convenio con Congelamiento", false, true),
    CRF("CRF", "Convenio con Refinanciamiento", false, true),
    CQT("CQT", "Convenio con Quita/Descuento", true, true),
    CAP("CAP", "Convenio Aprobado por Supervisor", false, true),
    CRC("CRC", "Cliente Rechaza Convenio Propuesto", false, false),
    CEV("CEV", "Convenio en Evaluación", false, true);

    private final String code;
    private final String description;
    private final Boolean requiresPayment;
    private final Boolean requiresSchedule;

    ManagementClassificationEnum(String code, String description, Boolean requiresPayment, Boolean requiresSchedule) {
        this.code = code;
        this.description = description;
        this.requiresPayment = requiresPayment;
        this.requiresSchedule = requiresSchedule;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getRequiresPayment() {
        return requiresPayment;
    }

    public Boolean getRequiresSchedule() {
        return requiresSchedule;
    }
}
