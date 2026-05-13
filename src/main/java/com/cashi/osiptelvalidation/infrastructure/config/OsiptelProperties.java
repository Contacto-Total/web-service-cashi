package com.cashi.osiptelvalidation.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo Osiptel.
 *
 * Bind con application.properties bajo el prefijo `cashi.osiptel.*`.
 *
 * legalReviewSignedOff: feature flag obligatorio. Si es false en environment 'prod',
 * los endpoints retornan HTTP 423 (Locked).
 */
@ConfigurationProperties(prefix = "cashi.osiptel")
public class OsiptelProperties {

    /** Si Legal aprobó el uso del portal. Bloquea endpoints en prod si está en false. */
    private boolean legalReviewSignedOff = false;

    /** Habilita el cron nocturno de selección. */
    private boolean candidateCronEnabled = false;

    /** Habilita el dispatcher que envía jobs al worker. */
    private boolean dispatcherEnabled = false;

    /** Cantidad máxima de candidatos a seleccionar por noche. */
    private int dailyQuota = 2200;

    /** Lote que el dispatcher reclama por ciclo. */
    private int dispatcherBatchSize = 20;

    /** Frecuencia del dispatcher (ms). 30s por default. */
    private long dispatcherIntervalMs = 30_000L;

    /** Máximo de intentos antes de marcar EXPIRED. */
    private int maxAttempts = 5;

    /** Umbral (min) para considerar un IN_PROGRESS como huérfano. */
    private int stuckThresholdMinutes = 5;

    /** URL del worker Node.js (HTTP). */
    private String workerUrl = "http://localhost:8090";

    /** Token compartido para autenticar al worker (header X-Worker-Token). */
    private String workerToken;

    /** Timeout total por check (ms). */
    private int workerTimeoutMs = 100_000;

    /** Sal para el hash del DNI. Idealmente por tenant; placeholder global por ahora. */
    private String dniHashSalt = "change-me";

    /** Cooldown tras OK (días). */
    private int cooldownOkDays = 90;

    /** Cooldown tras NOT_FOUND (días). */
    private int cooldownNotFoundDays = 30;

    /** Cooldown tras FAILED (días). */
    private int cooldownFailedDays = 1;

    // getters / setters
    public boolean isLegalReviewSignedOff() { return legalReviewSignedOff; }
    public void setLegalReviewSignedOff(boolean legalReviewSignedOff) { this.legalReviewSignedOff = legalReviewSignedOff; }

    public boolean isCandidateCronEnabled() { return candidateCronEnabled; }
    public void setCandidateCronEnabled(boolean candidateCronEnabled) { this.candidateCronEnabled = candidateCronEnabled; }

    public boolean isDispatcherEnabled() { return dispatcherEnabled; }
    public void setDispatcherEnabled(boolean dispatcherEnabled) { this.dispatcherEnabled = dispatcherEnabled; }

    public int getDailyQuota() { return dailyQuota; }
    public void setDailyQuota(int dailyQuota) { this.dailyQuota = dailyQuota; }

    public int getDispatcherBatchSize() { return dispatcherBatchSize; }
    public void setDispatcherBatchSize(int dispatcherBatchSize) { this.dispatcherBatchSize = dispatcherBatchSize; }

    public long getDispatcherIntervalMs() { return dispatcherIntervalMs; }
    public void setDispatcherIntervalMs(long dispatcherIntervalMs) { this.dispatcherIntervalMs = dispatcherIntervalMs; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public int getStuckThresholdMinutes() { return stuckThresholdMinutes; }
    public void setStuckThresholdMinutes(int stuckThresholdMinutes) { this.stuckThresholdMinutes = stuckThresholdMinutes; }

    public String getWorkerUrl() { return workerUrl; }
    public void setWorkerUrl(String workerUrl) { this.workerUrl = workerUrl; }

    public String getWorkerToken() { return workerToken; }
    public void setWorkerToken(String workerToken) { this.workerToken = workerToken; }

    public int getWorkerTimeoutMs() { return workerTimeoutMs; }
    public void setWorkerTimeoutMs(int workerTimeoutMs) { this.workerTimeoutMs = workerTimeoutMs; }

    public String getDniHashSalt() { return dniHashSalt; }
    public void setDniHashSalt(String dniHashSalt) { this.dniHashSalt = dniHashSalt; }

    public int getCooldownOkDays() { return cooldownOkDays; }
    public void setCooldownOkDays(int cooldownOkDays) { this.cooldownOkDays = cooldownOkDays; }

    public int getCooldownNotFoundDays() { return cooldownNotFoundDays; }
    public void setCooldownNotFoundDays(int cooldownNotFoundDays) { this.cooldownNotFoundDays = cooldownNotFoundDays; }

    public int getCooldownFailedDays() { return cooldownFailedDays; }
    public void setCooldownFailedDays(int cooldownFailedDays) { this.cooldownFailedDays = cooldownFailedDays; }
}
