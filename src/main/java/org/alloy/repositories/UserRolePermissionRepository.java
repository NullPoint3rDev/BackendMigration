package org.alloy.repositories;

import org.alloy.models.entities.UserRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRolePermissionRepository extends JpaRepository<UserRolePermission, Integer> {
    List<UserRolePermission> findByUserRoleId(Integer userRoleId);
}