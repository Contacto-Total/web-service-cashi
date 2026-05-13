package com.cashi.osiptelvalidation.infrastructure.health;

import com.cashi.osiptelvalidation.infrastructure.config.OsiptelProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HealthIndicator que consulta GET /healthz del worker Node.js.
 *
 * Se expone en /actuator/health/osiptelWorker.
 * Si el worker está caído, el indicator marca DOWN sin afectar al health global
 * (a menos que management.endpoint.health.group.* lo incluya explícitamente).
 */
@Component("osiptelWorker")
public class OsiptelWorkerHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;
    private final OsiptelProperties properties;

    public OsiptelWorkerHealthIndicator(@Qualifier("osiptelWorkerRestTemplate") RestTemplate restTemplate,
                                        OsiptelProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public Health health() {
        String url = properties.getWorkerUrl() + "/healthz";
        try {
            @SuppressWarnings("rawtypes")
            Map body = restTemplate.getForObject(url, Map.class);
            return Health.up()
                    .withDetail("url", url)
                    .withDetail("worker", body)
                    .build();
        } catch (RestClientException e) {
            return Health.down(e)
                    .withDetail("url", url)
                    .build();
        }
    }
}
