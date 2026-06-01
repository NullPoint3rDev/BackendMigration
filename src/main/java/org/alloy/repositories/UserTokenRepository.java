package org.alloy.repositories;

import org.alloy.models.entities.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Integer> {

    List<UserToken> findByUserAccountId(Integer userAccountId);

    Optional<UserToken> findByToken(UUID token);

    void deleteByUserAccountId(Integer userAccountId);

    void deleteByDateExpiredBefore(LocalDateTime date);

    @Query("SELECT t FROM UserToken t WHERE t.userAccountId = :userAccountId AND t.dateExpired > :currentDate")
    List<UserToken> findValidTokensByUserAccountId(@Param("userAccountId") Integer userAccountId, @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT COUNT(t) FROM UserToken t WHERE t.userAccountId = :userAccountId AND t.dateExpired > :currentDate")
    long countValidTokensByUserAccountId(@Param("userAccountId") Integer userAccountId, @Param("currentDate") LocalDateTime currentDate);
}
