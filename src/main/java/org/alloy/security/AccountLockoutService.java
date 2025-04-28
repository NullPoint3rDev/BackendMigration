package org.alloy.security;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountLockoutService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    // In a production environment, this should be stored in a database
    private final Map<String, UserLoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    public void recordFailedAttempt(String username) {
        UserLoginAttempt attempt = loginAttempts.getOrDefault(username, new UserLoginAttempt());
        attempt.incrementFailedAttempts();
        attempt.setLastFailedAttempt(LocalDateTime.now());
        loginAttempts.put(username, attempt);
    }

    public void resetFailedAttempts(String username) {
        loginAttempts.remove(username);
    }

    public boolean isAccountLocked(String username) {
        UserLoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null) {
            return false;
        }

        if (attempt.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            LocalDateTime lockoutEndTime = attempt.getLastFailedAttempt().plusMinutes(LOCKOUT_DURATION_MINUTES);
            if (LocalDateTime.now().isBefore(lockoutEndTime)) {
                return true;
            } else {
                // Lockout period has expired, reset the attempts
                loginAttempts.remove(username);
                return false;
            }
        }

        return false;
    }

    public int getRemainingAttempts(String username) {
        UserLoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null) {
            return MAX_FAILED_ATTEMPTS;
        }
        return Math.max(0, MAX_FAILED_ATTEMPTS - attempt.getFailedAttempts());
    }

    public LocalDateTime getLockoutEndTime(String username) {
        UserLoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null || attempt.getFailedAttempts() < MAX_FAILED_ATTEMPTS) {
            return null;
        }
        return attempt.getLastFailedAttempt().plusMinutes(LOCKOUT_DURATION_MINUTES);
    }

    private static class UserLoginAttempt {
        private int failedAttempts = 0;
        private LocalDateTime lastFailedAttempt;

        public void incrementFailedAttempts() {
            this.failedAttempts++;
        }

        public int getFailedAttempts() {
            return failedAttempts;
        }

        public LocalDateTime getLastFailedAttempt() {
            return lastFailedAttempt;
        }

        public void setLastFailedAttempt(LocalDateTime lastFailedAttempt) {
            this.lastFailedAttempt = lastFailedAttempt;
        }
    }
} 