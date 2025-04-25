package org.alloy.services;

import org.alloy.models.entities.UserToken;
import org.alloy.repositories.UserTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserTokenService {

    private final UserTokenRepository userTokenRepository;

    @Autowired
    public UserTokenService(UserTokenRepository userTokenRepository) {
        this.userTokenRepository = userTokenRepository;
    }

    public List<UserToken> getAllUserTokens() {
        return userTokenRepository.findAll();
    }

    public Optional<UserToken> getUserTokenById(Integer id) {
        return userTokenRepository.findById(id);
    }

    public List<UserToken> getUserTokensByUserId(Integer userId) {
        return userTokenRepository.findByUserId(userId);
    }

    public Optional<UserToken> getUserTokenByToken(String token) {
        return userTokenRepository.findByToken(token);
    }

    public UserToken createUserToken(UserToken userToken) {
        if (userToken.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (userToken.getToken() == null || userToken.getToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Token is required");
        }
        if (userToken.getExpirationDate() == null) {
            throw new IllegalArgumentException("Expiration date is required");
        }

        // Check if token already exists
        if (userTokenRepository.findByToken(userToken.getToken()).isPresent()) {
            throw new IllegalArgumentException("Token already exists");
        }

        userToken.setDateCreated(LocalDateTime.now());
        return userTokenRepository.save(userToken);
    }

    public UserToken updateUserToken(UserToken userToken) {
        if (userToken.getId() == null) {
            throw new IllegalArgumentException("Token ID is required");
        }
        if (userToken.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (userToken.getToken() == null || userToken.getToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Token is required");
        }
        if (userToken.getExpirationDate() == null) {
            throw new IllegalArgumentException("Expiration date is required");
        }

        // Check if token exists
        if (!userTokenRepository.existsById(userToken.getId())) {
            throw new IllegalArgumentException("Token not found");
        }

        // Check if new token value is already used by another token
        Optional<UserToken> existingToken = userTokenRepository.findByToken(userToken.getToken());
        if (existingToken.isPresent() && !existingToken.get().getId().equals(userToken.getId())) {
            throw new IllegalArgumentException("Token already exists");
        }

        return userTokenRepository.save(userToken);
    }

    public void deleteUserToken(Integer id) {
        if (!userTokenRepository.existsById(id)) {
            throw new IllegalArgumentException("Token not found");
        }
        userTokenRepository.deleteById(id);
    }

    public void deleteAllUserTokens(Integer userId) {
        userTokenRepository.deleteByUserId(userId);
    }

    public void deleteExpiredTokens() {
        userTokenRepository.deleteByExpirationDateBefore(LocalDateTime.now());
    }
}
