package org.alloy.models.entities;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notification")
@Data
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "UserAccountID", nullable = false)
    private Integer userAccountId;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "DateRead")
    private LocalDateTime dateRead;

    @Column(name = "Title", nullable = false)
    private String title;

    @Column(name = "Message", nullable = false)
    private String message;

    @Column(name = "Type", nullable = false)
    private String type;

    @Column(name = "IsRead")
    private Boolean isRead;

    @Column(name = "Link")
    private String link;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserAccountID", insertable = false, updatable = false)
    private UserAccount userAccount;
}
