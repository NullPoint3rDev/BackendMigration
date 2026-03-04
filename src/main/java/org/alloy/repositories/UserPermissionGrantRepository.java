package org.alloy.repositories;

import org.alloy.models.entities.UserPermissionGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionGrantRepository extends JpaRepository<UserPermissionGrant, Integer> {

    List<UserPermissionGrant> findByUserId(Long userId);

    @Query("SELECT g FROM UserPermissionGrant g WHERE g.userId = :userId AND g.userPermissionId = :permissionId")
    List<UserPermissionGrant> findByUserIdAndUserPermissionId(@Param("userId") Long userId, @Param("permissionId") Integer permissionId);
}
