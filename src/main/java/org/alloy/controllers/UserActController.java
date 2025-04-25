package org.alloy.controllers;

import org.alloy.models.entities.UserAct;
import org.alloy.services.UserActService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user-acts")
public class UserActController {

    private final UserActService userActService;

    @Autowired
    public UserActController(UserActService userActService) {
        this.userActService = userActService;
    }

    @GetMapping
    public ResponseEntity<List<UserAct>> getAllUserActs() {
        return ResponseEntity.ok(userActService.getAllUserActs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAct> getUserActById(@PathVariable Integer id) {
        return userActService.getUserActById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserAct>> getUserActsByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(userActService.getUserActsByUserId(userId));
    }

    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<UserAct>> getUserActsByUserIdAndType(
            @PathVariable Integer userId,
            @PathVariable String type) {
        return ResponseEntity.ok(userActService.getUserActsByUserIdAndType(userId, type));
    }

    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<List<UserAct>> getUserActsByUserIdAndDateRange(
            @PathVariable Integer userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(userActService.getUserActsByUserIdAndDateRange(userId, startDate, endDate));
    }

    @GetMapping("/user/{userId}/type/{type}/date-range")
    public ResponseEntity<List<UserAct>> getUserActsByUserIdAndTypeAndDateRange(
            @PathVariable Integer userId,
            @PathVariable String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(userActService.getUserActsByUserIdAndTypeAndDateRange(userId, type, startDate, endDate));
    }

    @GetMapping("/user/{userId}/type/{type}/count")
    public ResponseEntity<Long> countUserActsByUserIdAndTypeAndDateRange(
            @PathVariable Integer userId,
            @PathVariable String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(userActService.countUserActsByUserIdAndTypeAndDateRange(userId, type, startDate, endDate));
    }

    @PostMapping
    public ResponseEntity<UserAct> createUserAct(@RequestBody UserAct userAct) {
        try {
            return ResponseEntity.ok(userActService.createUserAct(userAct));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAct> updateUserAct(@PathVariable Integer id, @RequestBody UserAct userAct) {
        try {
            userAct.setId(id);
            return ResponseEntity.ok(userActService.updateUserAct(userAct));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserAct(@PathVariable Integer id) {
        try {
            userActService.deleteUserAct(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllUserActs(@PathVariable Integer userId) {
        userActService.deleteAllUserActs(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> cleanupOldUserActs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        userActService.cleanupOldUserActs(date);
        return ResponseEntity.ok().build();
    }
}
