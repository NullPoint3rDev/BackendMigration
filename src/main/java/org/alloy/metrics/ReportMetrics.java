package org.alloy.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Метрики генерации отчётов: количество (по типу/источнику/статусу) и длительность.
 *
 * Метрики:
 *  - wt2_report_generation_total{type, source, status}
 *  - wt2_report_generation_duration_seconds{type, source, status}
 *
 * type:   equipment | welder | wire-consumption | equipment-malfunction | other
 * source: manual | auto
 * status: success | failure
 */
@Component
public class ReportMetrics {

    private final MeterRegistry registry;

    public ReportMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(String type, String source, boolean success, long durationNanos) {
        String normalizedType = normalizeType(type);
        String status = success ? "success" : "failure";

        registry.counter("wt2_report_generation_total",
                "type", normalizedType,
                "source", source,
                "status", status).increment();

        Timer.builder("wt2_report_generation_duration_seconds")
                .description("Длительность генерации отчётов")
                .tags("type", normalizedType, "source", source, "status", status)
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /** Приводит разные варианты типа отчёта к одному из 4 поддерживаемых лейблов. */
    private String normalizeType(String type) {
        if (type == null) {
            return "other";
        }
        String t = type.trim().toLowerCase();
        switch (t) {
            case "wire_consumption":
            case "wire-consumption":
            case "materials":
            case "по расходу проволоки":
                return "wire-consumption";
            case "welder":
            case "welders":
            case "welder_work":
            case "по работе сварщика":
            case "по работе сварщика (швы)":
                return "welder";
            case "equipment":
            case "equipment_work":
            case "по работе оборудования":
            case "по работе оборудования (швы)":
                return "equipment";
            case "equipment-malfunction":
            case "equipment_malfunction":
            case "по неисправностям оборудования":
                return "equipment-malfunction";
            default:
                return "other";
        }
    }
}
