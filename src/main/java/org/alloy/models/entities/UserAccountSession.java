package org.alloy.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "UserAccountSession")
@Data
@NoArgsConstructor
public class UserAccountSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "UserAccountID", nullable = false)
    private Integer userAccountId;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "DateExpired")
    private LocalDateTime dateExpired;

    @Column(name = "DateLastActivity")
    private LocalDateTime dateLastActivity;

    @Column(name = "SessionToken", nullable = false)
    private UUID sessionToken;

    @Column(name = "IPAddress")
    private String ipAddress;

    @Column(name = "UserAgent")
    private String userAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserAccountID", insertable = false, updatable = false)
    private UserAccount userAccount;
}
