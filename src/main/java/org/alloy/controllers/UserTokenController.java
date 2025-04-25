package org.alloy.controllers;

import org.alloy.models.entities.UserToken;
import org.alloy.services.UserTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tokens")
public class UserTokenController {

    private final UserTokenService userTokenService;

    @Autowired
    public UserTokenController(UserTokenService userTokenService) {
        this.userTokenService = userTokenService;
    }

    @GetMapping
    public ResponseEntity<List<UserToken>> getAllTokens() {
        return ResponseEntity.ok(userTokenService.getAllUserTokens());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserToken> getTokenById(@PathVariable Integer id) {
        return userTokenService.getUserTokenById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserToken>> getTokensByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(userTokenService.getUserTokensByUserId(userId));
    }

    @GetMapping("/token/{token}")
    public ResponseEntity<UserToken> getTokenByTokenString(@PathVariable String token) {
        return userTokenService.getUserTokenByToken(token)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserToken> createToken(@RequestBody UserToken userToken) {
        try {
            return ResponseEntity.ok(userTokenService.createUserToken(userToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserToken> updateToken(@PathVariable Integer id, @RequestBody UserToken userToken) {
        try {
            userToken.setId(id);
            return ResponseEntity.ok(userTokenService.updateUserToken(userToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToken(@PathVariable Integer id) {
        try {
            userTokenService.deleteUserToken(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllUserTokens(@PathVariable Integer userId) {
        userTokenService.deleteAllUserTokens(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cleanup/expired")
    public ResponseEntity<Void> cleanupExpiredTokens() {
        userTokenService.deleteExpiredTokens();
        return ResponseEntity.ok().build();
    }
}
