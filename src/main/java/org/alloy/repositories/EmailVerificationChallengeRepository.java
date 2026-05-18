package org.alloy.repositories;

import org.alloy.models.entities.EmailVerificationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationChallengeRepository extends JpaRepository<EmailVerificationChallenge, Long> {

    Optional<EmailVerificationChallenge> findByUserAccountId(Integer userAccountId);

    void deleteByUserAccountId(Integer userAccountId);
}
