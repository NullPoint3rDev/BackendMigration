package org.alloy.models.entities;

import org.alloy.models.GeneralStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

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

    @JsonIgnore
    @Column(name = "PasswordHash")
    private byte[] passwordHash;

    @JsonIgnore
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

    @Column(name = "Workplace")
    private String workplace;

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

    @JsonManagedReference("inboxMessagesRef")
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InboxMessage> inboxMessages = new ArrayList<>();

    @JsonManagedReference("inboxMessagesReceivedRef")
    @OneToMany(mappedBy = "userAccountTo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InboxMessage> inboxMessagesReceived = new ArrayList<>();

    @JsonManagedReference("notificationsRef")
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @JsonBackReference("organizationRef")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrganizationID")
    private Organization organization;

    @JsonBackReference("organizationUnitRef")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrganizationUnitID", insertable = false, updatable = false)
    private OrganizationUnit organizationUnit;

    @JsonManagedReference("surveyPassesRef")
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurveyPass> surveyPasses = new ArrayList<>();

    @JsonBackReference("userRoleRef")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserRoleID", insertable = false, updatable = false)
    private UserRole userRole;

    @JsonManagedReference("userAccountSessionsRef")
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAccountSession> userAccountSessions = new ArrayList<>();

    @JsonManagedReference("userTokensRef")
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserToken> userTokens = new ArrayList<>();

    @JsonManagedReference("weldingLimitProgramsRef")
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeldingLimitProgram> weldingLimitPrograms = new ArrayList<>();

    @JsonManagedReference("userActsRef")
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAct> userActs = new ArrayList<>();
}
