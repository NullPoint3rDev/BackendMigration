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

    List<UserAct> findByUserId(Integer userId);

    List<UserAct> findByUserIdAndType(Integer userId, String type);

    @Query("SELECT u FROM UserAct u WHERE u.userId = :userId AND u.dateCreated BETWEEN :startDate AND :endDate ORDER BY u.dateCreated DESC")
    List<UserAct> findUserActsByUserIdAndDateRange(
            @Param("userId") Integer userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT u FROM UserAct u WHERE u.userId = :userId AND u.type = :type AND u.dateCreated BETWEEN :startDate AND :endDate ORDER BY u.dateCreated DESC")
    List<UserAct> findUserActsByUserIdAndTypeAndDateRange(
            @Param("userId") Integer userId,
            @Param("type") String type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(u) FROM UserAct u WHERE u.userId = :userId AND u.type = :type AND u.dateCreated BETWEEN :startDate AND :endDate")
    long countUserActsByUserIdAndTypeAndDateRange(
            @Param("userId") Integer userId,
            @Param("type") String type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    void deleteByUserId(Integer userId);

    void deleteByDateCreatedBefore(LocalDateTime date);
}
