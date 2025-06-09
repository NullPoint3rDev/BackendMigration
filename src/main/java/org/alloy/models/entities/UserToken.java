package org.alloy.models.entities;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "UserToken")
@Data
@NoArgsConstructor
public class UserToken {
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

    @Column(name = "Token", nullable = false)
    private UUID token;

    @Column(name = "Type", nullable = false)
    private String type;

    @Column(name = "Used")
    private Boolean used;

    @Column(name = "DateUsed")
    private LocalDateTime dateUsed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserAccountID", insertable = false, updatable = false)
    private UserAccount userAccount;
}
