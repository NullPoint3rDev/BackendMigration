package org.alloy.repositories;

import org.alloy.models.entities.UserAct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActRepository extends JpaRepository<UserAct, Integer> {

    List<UserAct> findByUserAccountId(Integer userAccountId);

    List<UserAct> findByUserAccountIdAndType(Integer userAccountId, String type);

    @Query("SELECT ua FROM UserAct ua WHERE ua.userAccountId = :userAccountId AND ua.dateCreated BETWEEN :startDate AND :endDate")
    List<UserAct> findUserActsByUserAccountIdAndDateRange(
            @Param("userAccountId") Integer userAccountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT ua FROM UserAct ua WHERE ua.userAccountId = :userAccountId AND ua.type = :type AND ua.dateCreated BETWEEN :startDate AND :endDate")
    List<UserAct> findUserActsByUserAccountIdAndTypeAndDateRange(
            @Param("userAccountId") Integer userAccountId,
            @Param("type") String type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(ua) FROM UserAct ua WHERE ua.userAccountId = :userAccountId AND ua.type = :type AND ua.dateCreated BETWEEN :startDate AND :endDate")
    long countUserActsByUserAccountIdAndTypeAndDateRange(
            @Param("userAccountId") Integer userAccountId,
            @Param("type") String type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    void deleteByUserAccountId(Integer userAccountId);

    void deleteByDateCreatedBefore(LocalDateTime date);
}
