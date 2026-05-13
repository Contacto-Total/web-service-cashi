package com.cashi.osiptelvalidation.application.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Métricas Micrometer del módulo Osiptel.
 * Mismo patrón que cashi-discador-backend/AuditoriaDiscadorService.
 *
 * Counters/Timers se exponen en /actuator/prometheus con prefijo `osiptel.`.
 */
@Service
public class OsiptelAuditService {

    private final MeterRegistry registry;

    private final ConcurrentMap<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> latencyTimers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> captchaCounters = new ConcurrentHashMap<>();

    public OsiptelAuditService(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Incrementa contador y registra latencia de una validación finalizada.
     */
    public void recordValidation(String status, String operator, long latencyMs) {
        String reqKey = status + "|" + (operator == null ? "NONE" : operator);
        requestCounters.computeIfAbsent(reqKey, k ->
                Counter.builder("osiptel.validation.requests")
                        .tag("status", status)
                        .tag("operator", operator == null ? "NONE" : operator)
                        .register(registry)
        ).increment();

        latencyTimers.computeIfAbsent(status, k ->
                Timer.builder("osiptel.validation.latency")
                        .tag("status", status)
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .register(registry)
        ).record(Duration.ofMillis(latencyMs));
    }

    public void recordCaptchaOutcome(String result) {
        captchaCounters.computeIfAbsent(result, k ->
                Counter.builder("osiptel.worker.captcha")
                        .tag("result", result)
                        .register(registry)
        ).increment();
    }

    public void recordReclaim(int count) {
        registry.counter("osiptel.dispatcher.reclaim", Tags.of("count_bucket", bucket(count))).increment(count);
    }

    public void recordEnqueue(int enqueued, int skipped) {
        registry.counter("osiptel.batch.enqueued").increment(enqueued);
        registry.counter("osiptel.batch.skipped").increment(skipped);
    }

    private static String bucket(int n) {
        if (n == 0) return "0";
        if (n < 10) return "<10";
        if (n < 100) return "<100";
        return ">=100";
    }
}
