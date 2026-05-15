package com.cashi.osiptel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente HTTP sincronico hacia cashi-osiptel-worker (Node.js + Playwright).
 *
 * El worker corre en la VM detras de una VPN con salida IP peruana
 * (ver cashi-osiptel-worker/docs/VPN-SETUP.md).
 *
 * Contrato del worker (no se modifica):
 *   POST /check
 *     body: { requestId, dni, dniType: "DNI" | "CE" | "PASAPORTE" | "RUC" }
 *     resp: { status: OK | NOT_FOUND | CAPTCHA_FAIL | BANNED | ERROR,
 *             lines: [{ phonePrefix, operator, modality }] | null }
 *
 * Este cliente:
 *  - Manda el documento al worker (NO el telefono).
 *  - Cuando llega la respuesta, hace MATCHING LOCAL: el telefono del cliente
 *    se compara contra los phonePrefix devueltos. Si matchea -> VALIDADO,
 *    sino NO_VALIDADO. Otros status del worker -> ERROR.
 */
@Configuration
@EnableConfigurationProperties(OsiptelProperties.class)
public class OsiptelClient {

    private static final Logger log = LoggerFactory.getLogger(OsiptelClient.class);

    private final RestTemplate restTemplate;
    private final OsiptelProperties props;

    public OsiptelClient(OsiptelProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(props.getWorkerTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Resultado consolidado (post-matching local).
     *  - status: VALIDADO | NO_VALIDADO | ERROR
     *  - operator: operador del portal de la linea que matcheo (null si NO_VALIDADO/ERROR)
     *  - errorDetail: mensaje legible si status=ERROR
     */
    public record CheckResult(String status, String operator, String errorDetail, long latencyMs) {}

    public CheckResult check(String phone, String dni) {
        String requestId = UUID.randomUUID().toString();
        long t0 = System.currentTimeMillis();
        String url = props.getWorkerUrl() + "/check";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (props.getWorkerToken() != null && !props.getWorkerToken().isBlank()) {
            headers.set("X-Worker-Token", props.getWorkerToken());
        }

        Map<String, Object> body = Map.of(
                "requestId", requestId,
                "dni", dni,
                "dniType", "DNI"
        );

        Map<?, ?> data;
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            data = resp.getBody();
        } catch (RestClientException e) {
            log.warn("Osiptel worker error: url={} requestId={} msg={}", url, requestId, e.getMessage());
            return new CheckResult("ERROR", null, e.getMessage(), System.currentTimeMillis() - t0);
        }

        if (data == null) {
            return new CheckResult("ERROR", null, "empty-payload", System.currentTimeMillis() - t0);
        }

        String workerStatus = (String) data.get("status");
        if (workerStatus == null) workerStatus = "ERROR";

        long elapsed = System.currentTimeMillis() - t0;

        // Mapeo a 3 estados del modelo NO-ortogonal:
        switch (workerStatus) {
            case "OK": {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> lines = (List<Map<String, Object>>) data.get("lines");
                return matchPhoneAgainstLines(phone, lines, elapsed);
            }
            case "NOT_FOUND":
                // DNI no figura en el portal -> no es titular de ninguna linea
                return new CheckResult("NO_VALIDADO", null, null, elapsed);
            default:
                // CAPTCHA_FAIL, BANNED, ERROR
                Object errObj = data.get("error");
                String errorDetail = errObj != null ? errObj.toString() : workerStatus;
                return new CheckResult("ERROR", null, errorDetail, elapsed);
        }
    }

    /**
     * Matching local: se compara el prefijo de {@code phone} contra los
     * phonePrefix devueltos por el portal. El portal devuelve los primeros 5
     * digitos visibles de cada linea (ej. "97851") seguidos de 4 enmascarados.
     */
    private CheckResult matchPhoneAgainstLines(String phone, List<Map<String, Object>> lines, long elapsed) {
        if (lines == null || lines.isEmpty()) {
            return new CheckResult("NO_VALIDADO", null, null, elapsed);
        }

        String prefix = mobilePrefix5(phone);
        if (prefix == null) {
            return new CheckResult("ERROR", null, "phone-format-invalid: " + phone, elapsed);
        }

        for (Map<String, Object> line : lines) {
            String linePrefix = (String) line.get("phonePrefix");
            if (linePrefix != null && linePrefix.equals(prefix)) {
                String operator = (String) line.get("operator");
                return new CheckResult("VALIDADO", operator, null, elapsed);
            }
        }
        return new CheckResult("NO_VALIDADO", null, null, elapsed);
    }

    /**
     * Normaliza el telefono y devuelve los primeros 5 digitos del numero movil
     * peruano (9XXXXXXXX). Acepta variantes con +51, espacios, guiones.
     * Devuelve null si no se puede extraer un movil PE valido.
     */
    static String mobilePrefix5(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        // Quitar codigo de pais 51 si aparece y deja 11 digitos
        if (digits.length() == 11 && digits.startsWith("51")) {
            digits = digits.substring(2);
        }
        // Movil PE: 9 digitos empezando con 9
        if (digits.length() != 9 || !digits.startsWith("9")) {
            return null;
        }
        return digits.substring(0, 5);
    }
}
