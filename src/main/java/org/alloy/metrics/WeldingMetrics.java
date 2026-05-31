package org.alloy.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Кастомные Prometheus-метрики, связанные с TCP-сервером сварочных аппаратов
 * (archive listener) и обработкой MAC-адресов.
 */
@Component
public class WeldingMetrics {

    private final AtomicInteger tcpActiveConnections = new AtomicInteger(0);
    private final Counter unknownMacCounter;

    public WeldingMetrics(MeterRegistry registry) {
        registry.gauge("wt2_tcp_active_connections", tcpActiveConnections);
        this.unknownMacCounter = Counter.builder("wt2_unknown_mac_total")
                .description("Количество пакетов от неизвестных/неразрешённых MAC-адресов")
                .register(registry);
    }

    /** Новое TCP-подключение аппарата открыто. */
    public void tcpConnectionOpened() {
        tcpActiveConnections.incrementAndGet();
    }

    /** TCP-подключение аппарата закрыто. */
    public void tcpConnectionClosed() {
        tcpActiveConnections.decrementAndGet();
    }

    /** Зафиксирован пакет от неизвестного/неразрешённого MAC. */
    public void recordUnknownMac() {
        unknownMacCounter.increment();
    }
}
