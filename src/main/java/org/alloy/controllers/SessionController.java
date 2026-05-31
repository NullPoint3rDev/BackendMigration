package org.alloy.controllers;

import org.alloy.security.SessionManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * Heartbeat активной сессии для метрики «реально онлайн».
 * Фронт вызывает раз в 5 минут, пока пользователь работает.
 */
@RestController
@RequestMapping("/session")
public class SessionController {

    @Autowired
    private SessionManagementService sessionManagementService;

    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        sessionManagementService.touchByUsername(authentication.getName());
        return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
    }
}
