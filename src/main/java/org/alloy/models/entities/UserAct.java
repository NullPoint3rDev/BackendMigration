package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "UserAct")
@Data
@NoArgsConstructor
public class UserAct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "UserAccountID", nullable = false)
    private Integer userAccountId;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "Type", nullable = false)
    private String type;

    @Column(name = "Description", nullable = false)
    private String description;

    @Column(name = "IPAddress")
    private String ipAddress;

    @Column(name = "UserAgent")
    private String userAgent;

    @Column(name = "EntityType")
    private String entityType;

    @Column(name = "EntityID")
    private Integer entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserAccountID", insertable = false, updatable = false)
    private UserAccount userAccount;
}
