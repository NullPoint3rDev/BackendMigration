package org.alloy.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.alloy.security.SessionManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Регистрирует gauge с количеством «реально онлайн» пользователей.
 *
 * Онлайн = сессия с heartbeat за последние {@code monitoring.online-session-window-minutes}
 * минут (по умолчанию 11 = 2 пропущенных 5-минутных heartbeat'а + запас).
 *
 * Метрика: wt2_online_sessions
 */
@Configuration
public class SessionMetricsConfig {

    public SessionMetricsConfig(MeterRegistry registry,
                                SessionManagementService sessions,
                                @Value("${monitoring.online-session-window-minutes:11}") long windowMinutes) {
        Duration window = Duration.ofMinutes(windowMinutes);
        Gauge.builder("wt2_online_sessions", sessions, s -> (double) s.countOnlineSessions(window))
                .description("Количество пользователей онлайн (heartbeat в пределах окна)")
                .register(registry);
    }
}
