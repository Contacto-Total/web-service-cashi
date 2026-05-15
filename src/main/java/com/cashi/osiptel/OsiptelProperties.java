package com.cashi.osiptel;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties del cliente Osiptel (modelo NO-ortogonal V17+).
 *
 * Bound desde application.properties:
 *   cashi.osiptel.worker-url=http://localhost:8090
 *   cashi.osiptel.worker-token=...
 *   cashi.osiptel.worker-timeout-ms=100000
 */
@ConfigurationProperties(prefix = "cashi.osiptel")
public class OsiptelProperties {

    private String workerUrl = "http://localhost:8090";
    private String workerToken = "";
    private int workerTimeoutMs = 100_000;

    public String getWorkerUrl() { return workerUrl; }
    public void setWorkerUrl(String workerUrl) { this.workerUrl = workerUrl; }

    public String getWorkerToken() { return workerToken; }
    public void setWorkerToken(String workerToken) { this.workerToken = workerToken; }

    public int getWorkerTimeoutMs() { return workerTimeoutMs; }
    public void setWorkerTimeoutMs(int workerTimeoutMs) { this.workerTimeoutMs = workerTimeoutMs; }
}
