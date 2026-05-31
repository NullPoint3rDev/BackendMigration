package org.alloy.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Счётчик ошибок backend уровня 5xx / необработанных исключений.
 *
 * Метрика: wt2_backend_errors_total{type}
 *   type = unhandled_exception
 */
@Component
public class BackendErrorMetrics {

    private final Counter unhandledExceptions;

    public BackendErrorMetrics(MeterRegistry registry) {
        this.unhandledExceptions = Counter.builder("wt2_backend_errors_total")
                .description("Необработанные исключения backend (HTTP 5xx)")
                .tag("type", "unhandled_exception")
                .register(registry);
    }

    public void recordUnhandledException() {
        unhandledExceptions.increment();
    }
}
