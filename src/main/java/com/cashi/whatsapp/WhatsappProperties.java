package com.cashi.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties del worker WhatsApp (modelo NO-ortogonal V17+).
 *
 * Bound desde application.properties:
 *   cashi.whatsapp.worker-token=...
 */
@ConfigurationProperties(prefix = "cashi.whatsapp")
public class WhatsappProperties {

    private String workerToken = "";

    public String getWorkerToken() { return workerToken; }
    public void setWorkerToken(String workerToken) { this.workerToken = workerToken; }
}
