package com.cashi.osiptelvalidation.application.internal.outboundservices;

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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente HTTP outbound al worker Node.js.
 *
 * Encapsula:
 *  - Serialización del request al contrato del worker.
 *  - Header de autenticación X-Worker-Token.
 *  - Manejo de timeouts (configurado en OsiptelAsyncConfig.osiptelWorkerRestTemplate).
 *  - Deserialización defensiva (si el worker devuelve estructura inesperada, marca ERROR).
 *
 * NO persiste nada - es solo el outbound. La persistencia ocurre en el dispatcher.
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
     * Llama a POST {workerUrl}/check.
     * El dni se envía solo para que el worker compute dniMatch; el worker NO retorna el nombre.
     *
     * @return resultado normalizado. Si el worker falla, status = "ERROR" con error_detail.
     */
    public WorkerCheckResult check(String requestId, String phone, String dni) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", requestId);
        body.put("phone", phone);
        body.put("dni", dni);

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
            Map<String, Object> payload = response.getBody();
            return WorkerCheckResult.fromPayload(response.getStatusCode().value(), latency, payload);
        } catch (RestClientException e) {
            long latency = System.currentTimeMillis() - t0;
            log.warn("Osiptel worker error tel=*** rid={} : {}", requestId, e.getMessage());
            return WorkerCheckResult.networkError(latency, e.getMessage());
        }
    }

    /**
     * Resultado normalizado del worker. Inmutable.
     *
     * status posible: OK | NOT_FOUND | CAPTCHA_FAIL | BANNED | ERROR.
     * operator/dniMatch solo presentes si status=OK.
     */
    public record WorkerCheckResult(
            int httpStatus,
            long latencyMs,
            String status,
            String operator,
            Boolean dniMatch,
            Integer captchaAttempts,
            String errorDetail
    ) {

        @SuppressWarnings("unchecked")
        public static WorkerCheckResult fromPayload(int httpStatus, long latencyMs, Map<String, Object> payload) {
            if (payload == null) {
                return new WorkerCheckResult(httpStatus, latencyMs, "ERROR", null, null, 0, "empty-payload");
            }
            String status = (String) payload.getOrDefault("status", "ERROR");
            String operator = (String) payload.get("operator");
            Object dniMatchRaw = payload.get("dniMatch");
            Boolean dniMatch = dniMatchRaw instanceof Boolean ? (Boolean) dniMatchRaw : null;
            Integer captcha = toInt(payload.get("captchaAttempts"));
            String error = (String) payload.get("error");
            return new WorkerCheckResult(httpStatus, latencyMs, status, operator, dniMatch, captcha, error);
        }

        public static WorkerCheckResult networkError(long latencyMs, String error) {
            return new WorkerCheckResult(0, latencyMs, "ERROR", null, null, 0, error);
        }

        private static Integer toInt(Object o) {
            if (o == null) return 0;
            if (o instanceof Number n) return n.intValue();
            try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return 0; }
        }
    }
}
