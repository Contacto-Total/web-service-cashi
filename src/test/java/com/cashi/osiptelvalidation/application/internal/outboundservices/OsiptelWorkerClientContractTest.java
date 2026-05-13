package com.cashi.osiptelvalidation.application.internal.outboundservices;

import com.cashi.osiptelvalidation.domain.model.valueobjects.DocumentType;
import com.cashi.osiptelvalidation.domain.model.valueobjects.OperatorCode;
import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Contract test del OsiptelWorkerClient (post-pivot: query por DNI, respuesta con lista de líneas).
 */
class OsiptelWorkerClientContractTest {

    private static final String WORKER_URL = "http://localhost:8090";
    private static final String WORKER_TOKEN = "test-shared-secret";

    private MockRestServiceServer mockServer;
    private OsiptelWorkerClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplateBuilder()
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
    void enviaDniYRecibeLista() {
        mockServer.expect(requestTo(WORKER_URL + "/check"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(header("X-Worker-Token", WORKER_TOKEN))
                .andExpect(jsonPath("$.requestId").value("rid-1"))
                .andExpect(jsonPath("$.dni").value("12345678"))
                .andExpect(jsonPath("$.dniType").value("DNI"))
                .andRespond(withSuccess(
                        """
                        {
                          "requestId": "rid-1",
                          "dni": "12345678",
                          "dniType": "DNI",
                          "status": "OK",
                          "lines": [
                            {"phonePrefix": "97851", "operator": "MOVISTAR", "modality": "CONTROL"},
                            {"phonePrefix": "98765", "operator": "CLARO", "modality": "POSTPAGO"}
                          ],
                          "latencyMs": 3456,
                          "captchaAttempts": 1,
                          "checkedAt": "2026-05-13T15:30:00Z"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        OsiptelWorkerClient.WorkerCheckResult result = client.check("rid-1", "12345678", DocumentType.DNI);

        assertEquals("OK", result.status());
        assertNotNull(result.lines());
        assertEquals(2, result.lines().size());
        assertEquals("97851", result.lines().get(0).phonePrefix());
        assertEquals(OperatorCode.MOVISTAR, result.lines().get(0).operator());
        assertEquals("CONTROL", result.lines().get(0).modality());
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
                          "dni": "12345678",
                          "dniType": "DNI",
                          "status": "NOT_FOUND",
                          "lines": [],
                          "latencyMs": 2000,
                          "captchaAttempts": 1,
                          "checkedAt": "2026-05-13T15:30:00Z"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        OsiptelWorkerClient.WorkerCheckResult result = client.check("rid-2", "12345678", DocumentType.DNI);

        assertEquals("NOT_FOUND", result.status());
        assertNotNull(result.lines());
        assertTrue(result.lines().isEmpty());
    }

    @Test
    void mapeaErrorDeRedAEstadoError() {
        mockServer.expect(requestTo(WORKER_URL + "/check"))
                .andRespond(withServerError());

        OsiptelWorkerClient.WorkerCheckResult result = client.check("rid-3", "12345678", DocumentType.DNI);

        assertEquals("ERROR", result.status());
        assertNull(result.lines());
        assertEquals(0, result.httpStatus());
        assertTrue(result.errorDetail() != null && !result.errorDetail().isBlank());
    }

    @Test
    void payloadVacioRetornaError() {
        mockServer.expect(requestTo(WORKER_URL + "/check"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        OsiptelWorkerClient.WorkerCheckResult result = client.check("rid-4", "12345678", DocumentType.DNI);

        assertEquals("ERROR", result.status());
        assertEquals("empty-payload", result.errorDetail());
    }

    @Test
    void lineaMalformadaSeIgnora() {
        mockServer.expect(requestTo(WORKER_URL + "/check"))
                .andRespond(withSuccess(
                        """
                        {
                          "requestId": "rid-5",
                          "dni": "12345678",
                          "dniType": "DNI",
                          "status": "OK",
                          "lines": [
                            {"phonePrefix": "97851", "operator": "MOVISTAR"},
                            {"operator": "CLARO"},
                            {"phonePrefix": "xxxxx", "operator": "NOEXISTE"}
                          ],
                          "latencyMs": 1000,
                          "captchaAttempts": 0,
                          "checkedAt": "2026-05-13T15:30:00Z"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        OsiptelWorkerClient.WorkerCheckResult result = client.check("rid-5", "12345678", DocumentType.DNI);

        assertEquals("OK", result.status());
        // Solo la primera linea es valida (las otras tienen prefix invalido o operator desconocido)
        assertEquals(1, result.lines().size());
        assertEquals("97851", result.lines().get(0).phonePrefix());
    }
}
