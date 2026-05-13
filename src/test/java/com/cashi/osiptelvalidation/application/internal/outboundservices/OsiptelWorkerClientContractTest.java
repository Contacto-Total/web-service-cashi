package com.cashi.osiptelvalidation.application.internal.outboundservices;

import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Test de contrato del OsiptelWorkerClient.
 *
 * Verifica sin levantar el worker real:
 *  - URL y método de la petición.
 *  - Headers (Content-Type, X-Worker-Token).
 *  - Body JSON (requestId, phone, dni).
 *  - Parsing de respuestas válidas e inválidas.
 *  - Comportamiento ante errores de red.
 *
 * No es @SpringBootTest - es JUnit puro con MockRestServiceServer.
 */
class OsiptelWorkerClientContractTest {

    private static final String WORKER_URL = "http://localhost:8090";
    private static final String WORKER_TOKEN = "test-shared-secret";

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private OsiptelWorkerClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .build();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        OsiptelProperties props = new OsiptelProperties();
        props.setWorkerUrl(WORKER_URL);
        props.setWorkerToken(WORKER_TOKEN);
        props.setWorkerTimeoutMs(2000);

        client = new OsiptelWorkerClient(restTemplate, props);
    }

    @Test
    void enviaRequestConTokenYRecibeOk() {
        mockServer.expect(requestTo(WORKER_URL + "/check"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(header("X-Worker-Token", WORKER_TOKEN))
                .andExpect(jsonPath("$.requestId").value("rid-1"))
                .andExpect(jsonPath("$.phone").value("987654321"))
                .andExpect(jsonPath("$.dni").value("12345678"))
                .andRespond(withSuccess(
                        """
                        {
                          "requestId": "rid-1",
                          "phone": "987654321",
                          "operator": "CLARO",
                          "dniMatch": true,
                          "status": "OK",
                          "latencyMs": 3456,
                          "captchaAttempts": 1,
                          "checkedAt": "2026-05-12T15:30:00Z"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        OsiptelWorkerClient.WorkerCheckResult result = client.check("rid-1", "987654321", "12345678");

        assertEquals("OK", result.status());
        assertEquals("CLARO", result.operator());
        assertEquals(Boolean.TRUE, result.dniMatch());
        assertEquals(1, result.captchaAttempts());
        assertEquals(200, result.httpStatus());
        mockServer.verify();
    }

    @Test
    void interpretaNotFoundComoStatusValido() {
        mockServer.expect(requestTo(WORKER_URL + "/check"))
                .andRespond(withSuccess(
                        """
                        {
                          "requestId": "rid-2",
                          "phone": "987654321",
                          "operator": null,
                          "dniMatch": null,
                          "status": "NOT_FOUND",
                          "latencyMs": 2000,
                          "captchaAttempts": 1,
                          "checkedAt": "2026-05-12T15:30:00Z"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        OsiptelWorkerClient.WorkerCheckResult result = client.check("rid-2", "987654321", "12345678");

        assertEquals("NOT_FOUND", result.status());
        assertNull(result.operator());
        assertNull(result.dniMatch());
    }

    @Test
    void mapeaErrorDeRedAEstadoError() {
        mockServer.expect(requestTo(WORKER_URL + "/check"))
                .andRespond(withServerError());

        OsiptelWorkerClient.WorkerCheckResult result = client.check("rid-3", "987654321", "12345678");

        assertEquals("ERROR", result.status());
        assertNull(result.operator());
        assertNull(result.dniMatch());
        assertEquals(0, result.httpStatus());
        assertTrue(result.errorDetail() != null && !result.errorDetail().isBlank());
    }

    @Test
    void payloadVacioRetornaError() {
        mockServer.expect(requestTo(WORKER_URL + "/check"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        OsiptelWorkerClient.WorkerCheckResult result = client.check("rid-4", "987654321", "12345678");

        assertEquals("ERROR", result.status());
        assertEquals("empty-payload", result.errorDetail());
    }
}
