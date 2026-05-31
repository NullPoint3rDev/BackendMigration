package org.alloy.security;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SessionManagementService {

    // In a production environment, this should be stored in a database
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, String> sessionTokens = new ConcurrentHashMap<>();

    public UUID createSession(String username, String userAgent, String ipAddress) {
        // Remove any existing sessions for this user
        removeUserSessions(username);

        UUID sessionId = UUID.randomUUID();
        UserSession session = new UserSession(username, sessionId, userAgent, ipAddress);

        activeSessions.put(username, session);
        sessionTokens.put(sessionId, username);

        return sessionId;
    }

    public void removeSession(UUID sessionId) {
        String username = sessionTokens.remove(sessionId);
        if (username != null) {
            activeSessions.remove(username);
        }
    }

    public void removeUserSessions(String username) {
        UserSession session = activeSessions.remove(username);
        if (session != null) {
            sessionTokens.remove(session.getSessionId());
        }
    }

    public boolean isValidSession(UUID sessionId) {
        String username = sessionTokens.get(sessionId);
        if (username == null) {
            return false;
        }

        UserSession session = activeSessions.get(username);
        if (session == null) {
            sessionTokens.remove(sessionId);
            return false;
        }

        // Check if session has expired (24 hours)
        if (session.getCreatedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            removeSession(sessionId);
            return false;
        }

        return true;
    }

    public UserSession getSession(UUID sessionId) {
        String username = sessionTokens.get(sessionId);
        if (username == null) {
            return null;
        }
        return activeSessions.get(username);
    }

    /** Имена пользователей с активной (не истёкшей) сессией. */
    public Set<String> getActiveUsernames() {
        purgeExpiredSessions();
        return Collections.unmodifiableSet(activeSessions.keySet());
    }

    /**
     * Обновляет время последней активности сессии пользователя (heartbeat).
     * @return true, если активная сессия найдена и обновлена.
     */
    public boolean touchByUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        UserSession session = activeSessions.get(username);
        if (session == null) {
            return false;
        }
        session.updateLastActivity();
        return true;
    }

    /**
     * Количество сессий, у которых последняя активность (heartbeat) была в пределах окна.
     * Используется как метрика «реально онлайн».
     */
    public long countOnlineSessions(java.time.Duration window) {
        purgeExpiredSessions();
        LocalDateTime threshold = LocalDateTime.now().minus(window);
        return activeSessions.values().stream()
                .filter(s -> s.getLastActivity().isAfter(threshold))
                .count();
    }

    public boolean isUserOnline(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        purgeExpiredSessions();
        UserSession session = activeSessions.get(username);
        if (session == null) {
            return false;
        }
        if (session.getCreatedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            removeUserSessions(username);
            return false;
        }
        return true;
    }

    private void purgeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        Set<String> expired = activeSessions.entrySet().stream()
                .filter(e -> e.getValue().getCreatedAt().plusHours(24).isBefore(now))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        expired.forEach(this::removeUserSessions);
    }

    public static class UserSession {
        private final String username;
        private final UUID sessionId;
        private final String userAgent;
        private final String ipAddress;
        private final LocalDateTime createdAt;
        private LocalDateTime lastActivity;

        public UserSession(String username, UUID sessionId, String userAgent, String ipAddress) {
            this.username = username;
            this.sessionId = sessionId;
            this.userAgent = userAgent;
            this.ipAddress = ipAddress;
            this.createdAt = LocalDateTime.now();
            this.lastActivity = this.createdAt;
        }

        public String getUsername() {
            return username;
        }

        public UUID getSessionId() {
            return sessionId;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public LocalDateTime getLastActivity() {
            return lastActivity;
        }

        public void updateLastActivity() {
            this.lastActivity = LocalDateTime.now();
        }
    }
} 