package org.alloy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class LogDirectoryInitializer {

    private static final Logger log = LoggerFactory.getLogger(LogDirectoryInitializer.class);

    @PostConstruct
    public void ensureLogsDirectory() {
        try {
            Path logsDir = Paths.get("logs");
            if (Files.notExists(logsDir)) {
                Files.createDirectories(logsDir);
                log.info("[LOGS] Создана директория логов: {}", logsDir.toAbsolutePath());
            } else {
                log.info("[LOGS] Директория логов уже существует: {}", logsDir.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("[LOGS] Не удалось подготовить директорию логов", e);
        }
    }
}
