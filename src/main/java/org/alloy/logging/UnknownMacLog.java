package org.alloy.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Отдельный лог неизвестных MAC (файл настраивается в logback-spring.xml).
 */
public final class UnknownMacLog {

    public static final String LOGGER_NAME = "org.alloy.tcp.unknown-mac";

    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);

    private UnknownMacLog() {
    }

    public static void unknownMac(String source, String mac, String detail) {
        if (detail == null || detail.isEmpty()) {
            LOG.warn("[{}] Unknown MAC: {}", source, mac);
        } else {
            LOG.warn("[{}] Unknown MAC: {} — {}", source, mac, detail);
        }
    }
}
