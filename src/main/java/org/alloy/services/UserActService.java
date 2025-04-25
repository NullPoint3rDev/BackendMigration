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

    public List<UserAct> getUserActsByUserId(Integer userAccountId) {
        return userActRepository.findByUserAccountId(userAccountId);
    }

    public List<UserAct> getUserActsByUserIdAndType(Integer userAccountId, String type) {
        return userActRepository.findByUserAccountIdAndType(userAccountId, type);
    }

    public List<UserAct> getUserActsByUserIdAndDateRange(Integer userAccountId, LocalDateTime startDate, LocalDateTime endDate) {
        return userActRepository.findUserActsByUserAccountIdAndDateRange(userAccountId, startDate, endDate);
    }

    public List<UserAct> getUserActsByUserIdAndTypeAndDateRange(
            Integer userAccountId, String type, LocalDateTime startDate, LocalDateTime endDate) {
        return userActRepository.findUserActsByUserAccountIdAndTypeAndDateRange(userAccountId, type, startDate, endDate);
    }

    public long countUserActsByUserIdAndTypeAndDateRange(
            Integer userAccountId, String type, LocalDateTime startDate, LocalDateTime endDate) {
        return userActRepository.countUserActsByUserAccountIdAndTypeAndDateRange(userAccountId, type, startDate, endDate);
    }

    public UserAct createUserAct(UserAct userAct) {
        if (userAct.getUserAccountId() == null) {
            throw new IllegalArgumentException("User Account ID is required");
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

    public void deleteAllUserActs(Integer userAccountId) {
        userActRepository.deleteByUserAccountId(userAccountId);
    }

    public void cleanupOldUserActs(LocalDateTime date) {
        userActRepository.deleteByDateCreatedBefore(date);
    }
}
