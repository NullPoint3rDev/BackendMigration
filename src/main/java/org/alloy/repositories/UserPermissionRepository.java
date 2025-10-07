package org.alloy.repositories;

import org.alloy.models.entities.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Integer> {
    Optional<UserPermission> findByName(String name);
} 