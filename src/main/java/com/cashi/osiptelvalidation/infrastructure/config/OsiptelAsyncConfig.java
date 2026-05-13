package com.cashi.osiptelvalidation.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuración del executor dedicado y del RestTemplate para Osiptel.
 *
 * Patrón: igual que el executor de AuditoriaDiscadorService - CallerRunsPolicy
 * para que si el pool se satura, el caller (dispatcher) ejecute síncrono
 * en lugar de descartar el job.
 */
@Configuration
@EnableConfigurationProperties(OsiptelProperties.class)
@EnableAsync
@EnableScheduling
public class OsiptelAsyncConfig {

    @Bean(name = "osiptelExecutor")
    public Executor osiptelExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("osiptel-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * RestTemplate dedicado para hablar con el worker.
     * Timeouts más generosos que el default porque el worker puede tardar hasta 90s
     * (captcha + Playwright).
     */
    @Bean(name = "osiptelWorkerRestTemplate")
    public RestTemplate osiptelWorkerRestTemplate(RestTemplateBuilder builder,
                                                  OsiptelProperties properties) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofMillis(properties.getWorkerTimeoutMs()))
                .build();
    }
}
