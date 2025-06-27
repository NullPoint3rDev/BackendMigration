package org.alloy.models.entities;

import org.alloy.models.GeneralStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "WeldingLimitProgram")
@Data
@NoArgsConstructor
public class WeldingLimitProgram {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "WeldingMachineID", nullable = false)
    private Integer weldingMachineId;

    @Column(name = "UserAccountID", nullable = false)
    private Integer userAccountId;

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

    @Column(name = "Settings")
    private String settings;

    @Column(name = "PropertyLimits")
    private String propertyLimits;

    @Column(name = "IsDefault")
    private Boolean isDefault;

    @JsonBackReference("limitProgramsRef")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WeldingMachineID", insertable = false, updatable = false)
    private WeldingMachine weldingMachine;

    @JsonBackReference("weldingLimitProgramsRef")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserAccountID", insertable = false, updatable = false)
    private UserAccount userAccount;

    @OneToMany(mappedBy = "weldingLimitProgram", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeldingMachineState> weldingMachineStates = new ArrayList<>();
}
