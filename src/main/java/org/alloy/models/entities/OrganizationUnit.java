package org.alloy.models.entities;

import org.alloy.models.GeneralStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "OrganizationUnit")
@Data
@NoArgsConstructor
public class OrganizationUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "OrganizationID", nullable = false)
    private Integer organizationId;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private GeneralStatus status;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = "Name", nullable = false)
    private String name;

    @Column(name = "Description")
    private String description;

    @Column(name = "Address")
    private String address;

    @Column(name = "Phone")
    private String phone;

    @Column(name = "Email")
    private String email;

    @Column(name = "ParentID")
    private Integer parentId;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrganizationID", insertable = false, updatable = false)
    private Organization organization;

    @JsonManagedReference
    @OneToMany(mappedBy = "organizationUnit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAccount> userAccounts = new ArrayList<>();

    @JsonManagedReference
    @OneToMany(mappedBy = "organizationUnit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeldingMachine> weldingMachines = new ArrayList<>();

    @Override
    public String toString() {
        return "OrganizationUnit{" +
                "id=" + id +
                ", organizationId=" + organizationId +
                ", status=" + status +
                ", dateCreated=" + dateCreated +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", parentId=" + parentId +
                '}';
    }
}
