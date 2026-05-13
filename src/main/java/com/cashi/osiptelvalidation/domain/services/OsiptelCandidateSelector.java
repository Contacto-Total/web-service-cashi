package com.cashi.osiptelvalidation.domain.services;

import com.cashi.osiptelvalidation.domain.model.commands.EnqueueOsiptelBatchCommand;

/**
 * Estrategia de selección de candidatos para validación nocturna.
 * Lee metodos_contacto + clientes, filtra por elegibilidad (móvil PE, DNI peruano,
 * sin validación activa, sin cooldown vigente), prioriza por subcartera activa y mora,
 * y devuelve un comando listo para encolar.
 */
public interface OsiptelCandidateSelector {

    /**
     * @param dailyQuota cantidad máxima de candidatos a seleccionar.
     * @return comando con los candidatos elegidos. Vacío si no hay candidatos.
     */
    EnqueueOsiptelBatchCommand selectCandidates(int dailyQuota);
}
