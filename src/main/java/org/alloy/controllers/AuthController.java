package org.alloy.controllers;

import org.alloy.models.entities.UserAccount;
import org.alloy.security.AccountLockedException;
import org.alloy.security.AuthenticationService;
import org.alloy.security.PasswordValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            AuthenticationService.AuthenticationResponse response = authenticationService.authenticate(
                    loginRequest.getUsername(), loginRequest.getPassword(), request);
            
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("token", response.getToken());
            responseMap.put("sessionId", response.getSessionId());
            
            return ResponseEntity.ok(responseMap);
        } catch (AccountLockedException e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Account Locked");
            errorMap.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorMap);
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Authentication Failed");
            errorMap.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMap);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String token) {
        try {
            // Extract the token from the Authorization header
            String jwt = token.substring(7); // Remove "Bearer " prefix
            authenticationService.logout(jwt);
            return ResponseEntity.ok().body(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Logout failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/validate-password")
    public ResponseEntity<?> validatePassword(@RequestBody PasswordValidationRequest request) {
        try {
            authenticationService.validatePassword(request.getPassword());
            return ResponseEntity.ok().body(Map.of("message", "Password is valid"));
        } catch (PasswordValidationException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Password Validation Failed");
            response.put("message", e.getMessage());
            response.put("validationErrors", e.getValidationErrors());
            return ResponseEntity.badRequest().body(response);
        }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class PasswordValidationRequest {
        private String password;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
} 