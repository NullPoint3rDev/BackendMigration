package org.alloy.services;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Извлекает один кадр {@code :MAC;HEX} из CONNECT-пачек и прочих ASCII-бlobs Core.
 */
public final class CoreAsciiFrameExtractor {

    /** :MAC;HEX — semicolon обязателен (отсекает битые {@code :MAC0000…}). */
    private static final Pattern CORE_FRAME = Pattern.compile(":([0-9A-Fa-f]{12});([0-9A-Fa-f]+)");

    private CoreAsciiFrameExtractor() {
    }

    /** Первые байты похожи на legacy ASCII Core (не v2 sync 0x01). */
    public static boolean looksLikeAsciiCoreChunk(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return false;
        }
        if (raw[0] == ':') {
            return true;
        }
        int scan = Math.min(raw.length, 256);
        String head = new String(raw, 0, scan, StandardCharsets.US_ASCII);
        if (head.startsWith("CONNECT ")) {
            return true;
        }
        return containsCoreFrameMarker(head);
    }

    static boolean containsCoreFrameMarker(String text) {
        return text != null && CORE_FRAME.matcher(text).find();
    }

    /**
     * CONNECT / multi-line blob → последний кадр, который реально парсится.
     * Одиночный {@code :MAC;HEX} возвращается как есть.
     */
    public static String pickLastParseableFrame(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String trimmed = raw.trim();
        String lastGood = null;

        if (trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0) {
            for (String line : trimmed.split("\\R")) {
                String candidate = toCoreFrame(line);
                if (candidate != null && CorePacketParser.parse(candidate) != null) {
                    lastGood = candidate;
                }
            }
            if (lastGood != null) {
                return lastGood;
            }
        }

        String direct = toCoreFrame(trimmed);
        if (direct != null && CorePacketParser.parse(direct) != null) {
            return direct;
        }

        Matcher matcher = CORE_FRAME.matcher(trimmed);
        while (matcher.find()) {
            String candidate = ":" + matcher.group(1).toUpperCase(Locale.ROOT) + ";" + matcher.group(2);
            if (CorePacketParser.parse(candidate) != null) {
                lastGood = candidate;
            }
        }
        return lastGood != null ? lastGood : trimmed;
    }

    static String toCoreFrame(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        Matcher matcher = CORE_FRAME.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        String mac = matcher.group(1).toUpperCase(Locale.ROOT);
        String hex = matcher.group(2);
        if (hex.length() < 8 || (hex.length() % 2) != 0) {
            return null;
        }
        return ":" + mac + ";" + hex;
    }
}
