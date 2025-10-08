package org.alloy.repositories;

import org.alloy.models.entities.UserRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRolePermissionRepository extends JpaRepository<UserRolePermission, Integer> {
    List<UserRolePermission> findByUserRoleId(Integer userRoleId);
    
    @Query("SELECT urp FROM UserRolePermission urp JOIN FETCH urp.userPermission WHERE urp.userRoleId = :userRoleId")
    List<UserRolePermission> findByUserRoleIdWithPermission(@Param("userRoleId") Integer userRoleId);
}