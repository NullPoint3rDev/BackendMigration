package org.alloy.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

@Entity
@Table(name = "InboxMessage")
@Data
@NoArgsConstructor
public class InboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "UserAccountID", nullable = false)
    private Integer userAccountId;

    @Column(name = "UserAccountToID", nullable = false)
    private Integer userAccountToId;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "DateRead")
    private LocalDateTime dateRead;

    @Column(name = "Subject", nullable = false)
    private String subject;

    @Column(name = "Message", nullable = false)
    private String message;

    @Column(name = "IsRead")
    private Boolean isRead;

    @Column(name = "IsDeleted")
    private Boolean isDeleted;

    @Column(name = "DateDeleted")
    private LocalDateTime dateDeleted;

    @Column(name = "Type")
    private String type;

    @JsonBackReference("inboxMessagesRef")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserAccountID", insertable = false, updatable = false)
    private UserAccount userAccount;

    @JsonBackReference("inboxMessagesReceivedRef")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserAccountToID", insertable = false, updatable = false)
    private UserAccount userAccountTo;
}
