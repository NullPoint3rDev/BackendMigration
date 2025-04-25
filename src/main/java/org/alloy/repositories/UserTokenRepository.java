package org.alloy.repositories;

import org.alloy.models.entities.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Integer> {

    List<UserToken> findByUserId(Integer userId);

    Optional<UserToken> findByToken(String token);

    void deleteByUserId(Integer userId);

    void deleteByExpirationDateBefore(LocalDateTime date);

    @Query("SELECT t FROM UserToken t WHERE t.userId = :userId AND t.expirationDate > :currentDate")
    List<UserToken> findValidTokensByUserId(@Param("userId") Integer userId, @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT COUNT(t) FROM UserToken t WHERE t.userId = :userId AND t.expirationDate > :currentDate")
    long countValidTokensByUserId(@Param("userId") Integer userId, @Param("currentDate") LocalDateTime currentDate);
}
