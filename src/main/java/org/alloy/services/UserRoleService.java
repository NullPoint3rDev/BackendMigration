package org.alloy.services;

import org.alloy.models.entities.UserRole;
import org.alloy.repositories.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

@Service
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public UserRoleService(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    public List<UserRole> getAllUserRoles() {
        // Ограничиваем количество записей для производительности
        return userRoleRepository.findAll().stream()
            .limit(50) // Максимум 50 ролей
            .collect(java.util.stream.Collectors.toList());
    }

    public Optional<UserRole> getUserRoleById(Integer id) {
        return userRoleRepository.findById(id);
    }

    public Optional<UserRole> getUserRoleByName(String name) {
        return userRoleRepository.findByName(name);
    }

    @Transactional
    public UserRole createUserRole(UserRole userRole) {
        return userRoleRepository.save(userRole);
    }

    @Transactional
    public UserRole updateUserRole(UserRole userRole) {
        if (!userRoleRepository.existsById(userRole.getId())) {
            throw new IllegalArgumentException("User role with ID " + userRole.getId() + " does not exist");
        }
        return userRoleRepository.save(userRole);
    }

    @Transactional
    public void deleteUserRole(Integer id) {
        userRoleRepository.deleteById(id);
    }

    public UserRole getDefaultRole() {
        return userRoleRepository.findByName("User").orElse(null);
    }

    // @PostConstruct
    // public void initializeDefaultData() {
    //     if(userRoleRepository.count() == 0) {
    //         createDefaultRoles();
    //     }
    // }
} 