package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Одноразовый код подтверждения email (6 цифр, хранится в виде SHA-256).
 */
@Entity
@Table(name = "email_verification_challenge", uniqueConstraints = {
        @UniqueConstraint(name = "uk_email_verification_user", columnNames = {"user_account_id"})
})
@Data
@NoArgsConstructor
public class EmailVerificationChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_account_id", nullable = false, unique = true)
    private Integer userAccountId;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
