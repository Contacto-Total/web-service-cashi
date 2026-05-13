package com.cashi.osiptelvalidation.application.internal.outboundservices;

import com.cashi.osiptelvalidation.domain.model.valueobjects.DocumentType;
import com.cashi.osiptelvalidation.domain.model.valueobjects.OperatorCode;
import com.cashi.osiptelvalidation.domain.model.valueobjects.OsiptelLine;
import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP outbound al worker Node.js.
 *
 * Post-pivot: envía DOCUMENTO y recibe LISTA DE LÍNEAS (no envía teléfono).
 */
@Component
public class OsiptelWorkerClient {

    private static final Logger log = LoggerFactory.getLogger(OsiptelWorkerClient.class);

    private final RestTemplate restTemplate;
    private final OsiptelProperties properties;

    public OsiptelWorkerClient(@Qualifier("osiptelWorkerRestTemplate") RestTemplate restTemplate,
                               OsiptelProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * POST {workerUrl}/check con el documento del cliente.
     * El DNI se envía plaintext (el worker lo usa solo para llenar el form);
     * el backend NO persiste el plaintext.
     */
    public WorkerCheckResult check(String requestId, String dni, DocumentType dniType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", requestId);
        body.put("dni", dni);
        body.put("dniType", dniType == null ? "DNI" : dniType.name());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getWorkerToken() != null && !properties.getWorkerToken().isBlank()) {
            headers.set("X-Worker-Token", properties.getWorkerToken());
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = properties.getWorkerUrl() + "/check";

        long t0 = System.currentTimeMillis();
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            long latency = System.currentTimeMillis() - t0;
            return WorkerCheckResult.fromPayload(response.getStatusCode().value(), latency, response.getBody());
        } catch (RestClientException e) {
            long latency = System.currentTimeMillis() - t0;
            log.warn("Osiptel worker error rid={}: {}", requestId, e.getMessage());
            return WorkerCheckResult.networkError(latency, e.getMessage());
        }
    }

    public record WorkerCheckResult(
            int httpStatus,
            long latencyMs,
            String status,            // OK | NOT_FOUND | CAPTCHA_FAIL | BANNED | ERROR
            List<OsiptelLine> lines,  // null si status != OK
            Integer captchaAttempts,
            String errorDetail
    ) {

        @SuppressWarnings("unchecked")
        public static WorkerCheckResult fromPayload(int httpStatus, long latencyMs, Map<String, Object> payload) {
            if (payload == null) {
                return new WorkerCheckResult(httpStatus, latencyMs, "ERROR", null, 0, "empty-payload");
            }
            String status = (String) payload.getOrDefault("status", "ERROR");
            Integer captcha = toInt(payload.get("captchaAttempts"));
            String error = (String) payload.get("error");

            List<OsiptelLine> lines = null;
            Object rawLines = payload.get("lines");
            if (rawLines instanceof List<?> list) {
                lines = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        try {
                            String prefix = (String) m.get("phonePrefix");
                            String opRaw = (String) m.get("operator");
                            String modality = (String) m.get("modality");
                            if (prefix == null || opRaw == null) continue;
                            lines.add(new OsiptelLine(prefix, OperatorCode.valueOf(opRaw), modality));
                        } catch (Exception e) {
                            // Skip línea malformada
                        }
                    }
                }
            }

            return new WorkerCheckResult(httpStatus, latencyMs, status, lines, captcha, error);
        }

        public static WorkerCheckResult networkError(long latencyMs, String error) {
            return new WorkerCheckResult(0, latencyMs, "ERROR", null, 0, error);
        }

        private static Integer toInt(Object o) {
            if (o == null) return 0;
            if (o instanceof Number n) return n.intValue();
            try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return 0; }
        }
    }
}
