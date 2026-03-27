package org.alloy.repositories;

import org.alloy.models.GeneralStatus;
import org.alloy.models.entities.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Integer> {

    Optional<UserAccount> findByUserName(String userName);

    Optional<UserAccount> findByUserNameAndStatusNot(String userName, GeneralStatus status);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByEmailAndStatusNot(String email, GeneralStatus status);

    List<UserAccount> findByOrganizationUnitId(Integer organizationUnitId);

    List<UserAccount> findByUserRoleId(Integer userRoleId);

    List<UserAccount> findByStatusNot(GeneralStatus status);

    @Query("SELECT ua FROM UserAccount ua WHERE ua.organizationUnitId = :organizationUnitId AND (ua.userName LIKE %:searchTerm% OR ua.name LIKE %:searchTerm% OR ua.email LIKE %:searchTerm%)")
    List<UserAccount> searchUserAccounts(@Param("organizationUnitId") Integer organizationUnitId, @Param("searchTerm") String searchTerm);

    @Query("SELECT ua FROM UserAccount ua WHERE ua.id IN :ids")
    List<UserAccount> findByIds(@Param("ids") List<Integer> ids);

    @Query("SELECT ua FROM UserAccount ua WHERE ua.userName = :userName AND ua.passwordHash = :passwordHash")
    Optional<UserAccount> findByUserNameAndPasswordHash(@Param("userName") String userName, @Param("passwordHash") byte[] passwordHash);

    @Query("SELECT ua FROM UserAccount ua LEFT JOIN FETCH ua.organization WHERE ua.userName = :userName AND ua.status <> :deleted")
    Optional<UserAccount> findActiveByUserNameWithOrganization(@Param("userName") String userName, @Param("deleted") GeneralStatus deleted);
}
