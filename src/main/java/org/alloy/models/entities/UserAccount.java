package org.alloy.models.entities;

import org.alloy.models.GeneralStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "UserAccount")
@Data
@NoArgsConstructor
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "OrganizationUnitID")
    private Integer organizationUnitId;

    @Column(name = "UserRoleID", nullable = false)
    private Integer userRoleId;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private GeneralStatus status;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "DateLastLogon")
    private LocalDateTime dateLastLogon;

    @Column(name = "UserName", nullable = false, unique = true)
    private String userName;

    @Column(name = "PasswordHash")
    private byte[] passwordHash;

    @Column(name = "PasswordSalt")
    private String passwordSalt;

    @Column(name = "Name")
    private String name;

    @Column(name = "FailedLoginsCount")
    private Integer failedLoginsCount;

    @Column(name = "Email")
    private String email;

    @Column(name = "Position")
    private String position;

    @Column(name = "Category")
    private String category;

    @Column(name = "PersonnelNumber")
    private String personnelNumber;

    @Column(name = "RFID")
    private String rfid;

    @Column(name = "RecruitmentDate")
    private LocalDateTime recruitmentDate;

    @Column(name = "BirthDate")
    private LocalDateTime birthDate;

    @Column(name = "Photo")
    private UUID photo;

    @Column(name = "Education")
    private String education;

    @Column(name = "Phone")
    private String phone;

    @Column(name = "Address")
    private String address;

    @Column(name = "Description")
    private String description;

    @Column(name = "RFID_Hex")
    private String rfidHex;

    @Column(name = "AllowEmailNotifications")
    private Boolean allowEmailNotifications;

    @Column(name = "WorkerAttestationDate")
    private LocalDateTime workerAttestationDate;

    @Column(name = "WorkerNextAttestationDate")
    private LocalDateTime workerNextAttestationDate;

    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InboxMessage> inboxMessages = new ArrayList<>();

    @OneToMany(mappedBy = "userAccountTo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InboxMessage> inboxMessagesReceived = new ArrayList<>();

    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Organization> organizations = new ArrayList<>();

    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrganizationUnit> organizationUnits = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrganizationUnitID", insertable = false, updatable = false)
    private OrganizationUnit organizationUnit;

    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurveyPass> surveyPasses = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserRoleID", insertable = false, updatable = false)
    private UserRole userRole;

    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAccountSession> userAccountSessions = new ArrayList<>();

    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserToken> userTokens = new ArrayList<>();

    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeldingLimitProgram> weldingLimitPrograms = new ArrayList<>();

    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAct> userActs = new ArrayList<>();
}
