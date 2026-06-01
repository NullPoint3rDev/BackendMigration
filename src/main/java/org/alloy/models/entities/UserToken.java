package org.alloy.models.entities;

import javax.persistence.*;
import org.alloy.models.converters.UuidAttributeConverter;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;

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

    @Convert(converter = UuidAttributeConverter.class)
    @Column(name = "Token", nullable = false, length = 36)
    private UUID token;

    @Column(name = "Type", nullable = false)
    private String type;

    @Column(name = "Used")
    private Boolean used;

    @Column(name = "DateUsed")
    private LocalDateTime dateUsed;

    @JsonBackReference("userTokensRef")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserAccountID", insertable = false, updatable = false)
    private UserAccount userAccount;
}
