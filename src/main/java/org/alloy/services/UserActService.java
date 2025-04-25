package org.alloy.services;

import org.alloy.models.entities.UserAct;
import org.alloy.repositories.UserActRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserActService {

    private final UserActRepository userActRepository;

    @Autowired
    public UserActService(UserActRepository userActRepository) {
        this.userActRepository = userActRepository;
    }

    public List<UserAct> getAllUserActs() {
        return userActRepository.findAll();
    }

    public Optional<UserAct> getUserActById(Integer id) {
        return userActRepository.findById(id);
    }

    public List<UserAct> getUserActsByUserId(Integer userId) {
        return userActRepository.findByUserId(userId);
    }

    public List<UserAct> getUserActsByUserIdAndType(Integer userId, String type) {
        return userActRepository.findByUserIdAndType(userId, type);
    }

    public List<UserAct> getUserActsByUserIdAndDateRange(Integer userId, LocalDateTime startDate, LocalDateTime endDate) {
        return userActRepository.findUserActsByUserIdAndDateRange(userId, startDate, endDate);
    }

    public List<UserAct> getUserActsByUserIdAndTypeAndDateRange(
            Integer userId, String type, LocalDateTime startDate, LocalDateTime endDate) {
        return userActRepository.findUserActsByUserIdAndTypeAndDateRange(userId, type, startDate, endDate);
    }

    public long countUserActsByUserIdAndTypeAndDateRange(
            Integer userId, String type, LocalDateTime startDate, LocalDateTime endDate) {
        return userActRepository.countUserActsByUserIdAndTypeAndDateRange(userId, type, startDate, endDate);
    }

    public UserAct createUserAct(UserAct userAct) {
        if (userAct.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (userAct.getType() == null || userAct.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Type is required");
        }
        if (userAct.getDescription() == null || userAct.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }

        userAct.setDateCreated(LocalDateTime.now());
        return userActRepository.save(userAct);
    }

    public UserAct updateUserAct(UserAct userAct) {
        if (userAct.getId() == null) {
            throw new IllegalArgumentException("User Act ID is required");
        }

        UserAct existingUserAct = userActRepository.findById(userAct.getId())
                .orElseThrow(() -> new IllegalArgumentException("User Act not found"));

        userAct.setDateCreated(existingUserAct.getDateCreated());
        return userActRepository.save(userAct);
    }

    public void deleteUserAct(Integer id) {
        if (!userActRepository.existsById(id)) {
            throw new IllegalArgumentException("User Act not found");
        }
        userActRepository.deleteById(id);
    }

    public void deleteAllUserActs(Integer userId) {
        userActRepository.deleteByUserId(userId);
    }

    public void cleanupOldUserActs(LocalDateTime date) {
        userActRepository.deleteByDateCreatedBefore(date);
    }
}
