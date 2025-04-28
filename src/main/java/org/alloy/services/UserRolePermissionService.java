package org.alloy.services;

import org.alloy.models.entities.UserRolePermission;
import org.alloy.repositories.UserRolePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserRolePermissionService {
    @Autowired
    private UserRolePermissionRepository userRolePermissionRepository;

    public List<UserRolePermission> findAll() {
        return userRolePermissionRepository.findAll();
    }

    public Optional<UserRolePermission> findById(Integer id) {
        return userRolePermissionRepository.findById(id);
    }

    public UserRolePermission save(UserRolePermission userRolePermission) {
        return userRolePermissionRepository.save(userRolePermission);
    }

    public void deleteById(Integer id) {
        userRolePermissionRepository.deleteById(id);
    }
} 