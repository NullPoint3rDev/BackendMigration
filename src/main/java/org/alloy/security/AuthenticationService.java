package org.alloy.security;

import org.alloy.models.entities.UserAccount;
import org.alloy.services.UserAccountService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider tokenProvider;
    private final AccountLockoutService accountLockoutService;
    private final SessionManagementService sessionManagementService;
    private final PasswordValidationService passwordValidationService;
    private final UserAccountService userAccountService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtTokenProvider tokenProvider,
            AccountLockoutService accountLockoutService,
            SessionManagementService sessionManagementService,
            PasswordValidationService passwordValidationService,
            UserAccountService userAccountService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.tokenProvider = tokenProvider;
        this.accountLockoutService = accountLockoutService;
        this.sessionManagementService = sessionManagementService;
        this.passwordValidationService = passwordValidationService;
        this.userAccountService = userAccountService;
    }

    public AuthenticationResponse authenticate(String username, String password, HttpServletRequest request) {
        // Check if account is locked
        if (accountLockoutService.isAccountLocked(username)) {
            LocalDateTime lockoutEndTime = accountLockoutService.getLockoutEndTime(username);
            throw new AccountLockedException("Account is locked until " + lockoutEndTime);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Generate JWT token
            String jwt = tokenProvider.generateToken(authentication);
            
            // Create session
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = getClientIp(request);
            UUID sessionId = sessionManagementService.createSession(username, userAgent, ipAddress);
            
            // Reset failed attempts on successful login
            accountLockoutService.resetFailedAttempts(username);
            
            return new AuthenticationResponse(jwt, sessionId.toString());
        } catch (AuthenticationException e) {
            // Record failed attempt
            accountLockoutService.recordFailedAttempt(username);
            
            // Check if account is now locked
            if (accountLockoutService.isAccountLocked(username)) {
                LocalDateTime lockoutEndTime = accountLockoutService.getLockoutEndTime(username);
                throw new AccountLockedException("Account is now locked until " + lockoutEndTime);
            }
            
            int remainingAttempts = accountLockoutService.getRemainingAttempts(username);
            throw new AuthenticationException("Invalid username or password. " + remainingAttempts + " attempts remaining.") {};
        }
    }

    public void validatePassword(String password) {
        List<String> errors = passwordValidationService.validatePassword(password);
        if (!errors.isEmpty()) {
            throw new PasswordValidationException("Password validation failed", errors);
        }
    }

    public void logout(String token) {
        // Extract session ID from token or use token directly
        UUID sessionId = UUID.fromString(token);
        sessionManagementService.removeSession(sessionId);
        SecurityContextHolder.clearContext();
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userDetailsService.loadUserByUsername(username);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    public static class AuthenticationResponse {
        private final String token;
        private final String sessionId;

        public AuthenticationResponse(String token, String sessionId) {
            this.token = token;
            this.sessionId = sessionId;
        }

        public String getToken() {
            return token;
        }

        public String getSessionId() {
            return sessionId;
        }
    }
} 