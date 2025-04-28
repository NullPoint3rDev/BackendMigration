package org.alloy.services;

import org.alloy.models.entities.UserPermission;
import org.alloy.repositories.UserPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserPermissionService {
    @Autowired
    private UserPermissionRepository userPermissionRepository;

    public List<UserPermission> findAll() {
        return userPermissionRepository.findAll();
    }

    public Optional<UserPermission> findById(Integer id) {
        return userPermissionRepository.findById(id);
    }

    public UserPermission save(UserPermission userPermission) {
        return userPermissionRepository.save(userPermission);
    }

    public void deleteById(Integer id) {
        userPermissionRepository.deleteById(id);
    }
} 