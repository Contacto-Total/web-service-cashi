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
 * HealthIndicator que consulta GET /healthz del worker (legacy push-based).
 *
 * NOTA post-pivot: en el modelo pull-based actual (Electron app), no hay un
 * "worker remoto" al cual hacer ping desde el backend - es el Electron el que
 * inicia la conexión. Este indicator queda como infraestructura disponible
 * por si se requiere monitorear algún otro endpoint HTTP en el futuro.
 *
 * Si configuras `cashi.osiptel.worker-url` apuntando a un workspace de
 * validación HTTP cualquiera, este indicator lo monitorea.
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
