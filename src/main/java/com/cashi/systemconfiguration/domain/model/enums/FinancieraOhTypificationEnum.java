package com.cashi.systemconfiguration.domain.model.enums;

import lombok.Getter;

@Getter
public enum FinancieraOhTypificationEnum {
    // ============================================================================
    // NIVEL 1: RESULTADO POSITIVO (RP)
    // ============================================================================
    RP("RP", "Resultado Positivo", 1, null, "#10B981", "check-circle"),

    // NIVEL 2: 1.1. Pago Confirmado (PC)
    PC("PC", "Pago Confirmado", 2, "RP", "#059669", "dollar-sign"),

    // NIVEL 3: Hijos de Pago Confirmado
    PT("PT", "Pago Total", 3, "PC", "#10B981", "check-circle"),
    PP("PP", "Pago Parcial", 3, "PC", "#34D399", "circle-half"),
    PPT("PPT", "Pago por Tercero", 3, "PC", "#6EE7B7", "users"),

    // NIVEL 2: 1.2. Promesa de Pago (PDP)
    PDP("PDP", "Promesa de Pago", 2, "RP", "#F59E0B", "handshake"),

    // NIVEL 3: Hijos de Promesa de Pago
    PU("PU", "Pago Único", 3, "PDP", "#FBBF24", "calendar-check"),
    PF("PF", "Pago Fraccionado", 3, "PDP", "#FCD34D", "calendar-days"),
    CF("CF", "Convenio Formal", 3, "PDP", "#FDE68A", "file-signature"),

    // NIVEL 2: 1.3. Excepción Autorizada (EA)
    EA("EA", "Excepción Autorizada", 2, "RP", "#8B5CF6", "shield-check"),

    // NIVEL 3: Hijos de Excepción Autorizada
    EA_ENF("EA_ENF", "Enfermedad", 3, "EA", "#A78BFA", "heart-pulse"),
    EA_DES("EA_DES", "Desempleo", 3, "EA", "#C4B5FD", "briefcase-off"),
    EA_LEG("EA_LEG", "Situación Legal", 3, "EA", "#DDD6FE", "gavel"),

    // ============================================================================
    // NIVEL 1: CONTACTO SIN ACUERDO (CSA)
    // ============================================================================
    CSA("CSA", "Contacto sin Acuerdo", 1, null, "#EF4444", "x-circle"),

    // NIVEL 2: 2.1. Cliente se Niega (CN)
    CN("CN", "Cliente se Niega", 2, "CSA", "#DC2626", "ban"),

    // NIVEL 3: Hijos de Cliente se Niega
    CN_RN("CN_RN", "Rechaza negociación", 3, "CN", "#EF4444", "hand-raised"),
    CN_NIP("CN_NIP", "No tiene intención de pagar", 3, "CN", "#F87171", "circle-slash"),
    CN_CI("CN_CI", "Comportamiento inadecuado", 3, "CN", "#FCA5A5", "user-x"),

    // NIVEL 2: 2.2. Cliente con Dificultad (CD)
    CD("CD", "Cliente con Dificultad", 2, "CSA", "#FB923C", "alert-circle"),

    // NIVEL 3: Hijos de Cliente con Dificultad
    CD_SDA("CD_SDA", "Sin dinero actual", 3, "CD", "#FDBA74", "wallet-off"),
    CD_SMT("CD_SMT", "Solicita más tiempo", 3, "CD", "#FED7AA", "clock"),
    CD_RR("CD_RR", "Requiere reestructuración", 3, "CD", "#FFEDD5", "file-edit"),

    // NIVEL 2: 2.3. Promesa Incumplida (PI)
    PI("PI", "Promesa Incumplida", 2, "CSA", "#DC2626", "calendar-x"),

    // ============================================================================
    // NIVEL 1: SIN CONTACTO (SC)
    // ============================================================================
    SC("SC", "Sin Contacto", 1, null, "#64748B", "phone-off"),

    // NIVEL 2: 3.1. Sin Respuesta (SR)
    SR("SR", "Sin Respuesta", 2, "SC", "#475569", "phone-missed"),

    // NIVEL 3: Hijos de Sin Respuesta
    SR_NC("SR_NC", "No contesta", 3, "SR", "#64748B", "phone-x"),
    SR_BV("SR_BV", "Buzón de voz", 3, "SR", "#94A3B8", "voicemail"),
    SR_OC("SR_OC", "Ocupado/Colgó", 3, "SR", "#CBD5E1", "phone-incoming"),

    // NIVEL 2: 3.2. Número Inválido (NI)
    NI("NI", "Número Inválido", 2, "SC", "#71717A", "phone-slash"),

    // NIVEL 3: Hijos de Número Inválido
    NI_AFS("NI_AFS", "Apagado/Fuera de servicio", 3, "NI", "#A1A1AA", "power-off"),
    NI_NE("NI_NE", "Número equivocado", 3, "NI", "#D4D4D8", "hash"),
    NI_NEX("NI_NEX", "No existe", 3, "NI", "#E4E4E7", "circle-off"),

    // ============================================================================
    // NIVEL 1: GESTIÓN ADMINISTRATIVA (GA)
    // ============================================================================
    GA("GA", "Gestión Administrativa", 1, null, "#3B82F6", "clipboard-list"),

    // NIVEL 2: 4.1. Actualización de Datos (AD)
    AD("AD", "Actualización de Datos", 2, "GA", "#2563EB", "edit"),

    // NIVEL 3: Hijos de Actualización de Datos
    AD_NT("AD_NT", "Nuevo teléfono", 3, "AD", "#60A5FA", "phone-plus"),
    AD_ND("AD_ND", "Nueva dirección", 3, "AD", "#93C5FD", "map-pin"),
    AD_NC("AD_NC", "Nuevo contacto", 3, "AD", "#BFDBFE", "user-plus"),

    // NIVEL 2: 4.2. Escalamiento (ESC)
    ESC("ESC", "Escalamiento", 2, "GA", "#1E40AF", "arrow-up-circle"),

    // NIVEL 3: Hijos de Escalamiento
    ESC_LEG("ESC_LEG", "A Legal", 3, "ESC", "#3B82F6", "scale"),
    ESC_SUP("ESC_SUP", "A Supervisor", 3, "ESC", "#60A5FA", "user-shield"),
    ESC_JUD("ESC_JUD", "Acciones judiciales", 3, "ESC", "#93C5FD", "gavel");

    private final String code;
    private final String description;
    private final Integer hierarchyLevel;
    private final String parentCode;
    private final String colorHex;
    private final String iconName;

    FinancieraOhTypificationEnum(String code, String description, Integer hierarchyLevel,
                                    String parentCode, String colorHex, String iconName) {
        this.code = code;
        this.description = description;
        this.hierarchyLevel = hierarchyLevel;
        this.parentCode = parentCode;
        this.colorHex = colorHex;
        this.iconName = iconName;
    }

    /**
     * Determina si esta clasificación requiere captura de pago
     */
    public Boolean requiresPayment() {
        return code.equals("PC") || code.equals("PT") || code.equals("PP") || code.equals("PPT");
    }

    /**
     * Determina si esta clasificación requiere cronograma de pago
     */
    public Boolean requiresSchedule() {
        return code.equals("PDP") || code.equals("PF") || code.equals("CF");
    }

    /**
     * Determina si esta clasificación requiere seguimiento
     */
    public Boolean requiresFollowUp() {
        return code.equals("PDP") || code.equals("PU") || code.equals("PF") ||
               code.equals("CF") || code.equals("PI") || code.equals("CD") ||
               code.equals("CD_SMT") || code.equals("CD_RR");
    }

    /**
     * Determina si esta clasificación indica un resultado exitoso
     */
    public Boolean isSuccessful() {
        return hierarchyLevel == 1 && code.equals("RP");
    }

    /**
     * Determina si esta clasificación aplica pagos a cronogramas pendientes
     * Cuando se registra un pago con estas clasificaciones, el sistema busca automáticamente
     * cronogramas pendientes del cliente y aplica el pago a las cuotas en orden
     */
    public Boolean appliesPaymentToSchedule() {
        // PC (Pago Confirmado), PT (Pago Total), PP (Pago Parcial), PPT (Pago por Tercero)
        return code.equals("PC") || code.equals("PT") || code.equals("PP") || code.equals("PPT");
    }

    /**
     * Obtiene la categoría principal de esta clasificación
     */
    public String getMainCategory() {
        if (code.equals("RP") || parentCode != null && (parentCode.equals("RP") ||
            parentCode.equals("PC") || parentCode.equals("PDP") || parentCode.equals("EA"))) {
            return "RESULTADO_POSITIVO";
        } else if (code.equals("CSA") || parentCode != null && (parentCode.equals("CSA") ||
            parentCode.equals("CN") || parentCode.equals("CD") || parentCode.equals("PI"))) {
            return "CONTACTO_SIN_ACUERDO";
        } else if (code.equals("SC") || parentCode != null && (parentCode.equals("SC") ||
            parentCode.equals("SR") || parentCode.equals("NI"))) {
            return "SIN_CONTACTO";
        } else if (code.equals("GA") || parentCode != null && (parentCode.equals("GA") ||
            parentCode.equals("AD") || parentCode.equals("ESC"))) {
            return "GESTION_ADMINISTRATIVA";
        }
        return "OTROS";
    }
}
