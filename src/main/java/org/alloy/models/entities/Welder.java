package org.alloy.models.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Welders")
@Data
@NoArgsConstructor
@Schema(description = "Сварщик")
public class Welder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор сварщика")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    @Schema(description = "ФИО сварщика")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Статус сварщика")
    private WelderStatus status;

    @Column(name = "department", length = 255)
    @Schema(description = "Подразделение")
    private String department;

    @Column(name = "position", length = 255)
    @Schema(description = "Должность")
    private String position;

    @Column(name = "grade", length = 50)
    @Schema(description = "Разряд")
    private String grade;

    @Column(name = "employee_id", length = 100)
    @Schema(description = "Табельный номер")
    private String employeeId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "hire_date")
    @Schema(description = "Дата приема на работу")
    private LocalDate hireDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "birth_date")
    @Schema(description = "Дата рождения")
    private LocalDate birthDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "certification_date")
    @Schema(description = "Дата аттестации")
    private LocalDate certificationDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "next_certification_date")
    @Schema(description = "Дата следующей аттестации")
    private LocalDate nextCertificationDate;

    @Column(name = "phone", length = 50)
    @Schema(description = "Номер телефона")
    private String phone;

    @Column(name = "address", length = 500)
    @Schema(description = "Адрес")
    private String address;

    @Column(name = "rfid_code", length = 100)
    @Schema(description = "Код бесконтактной карты (RFID)")
    private String rfidCode;

    @Column(name = "education", length = 1000)
    @Schema(description = "Сведения об образовании")
    private String education;

    @Column(name = "email", length = 255)
    @Schema(description = "Email")
    private String email;

    @Column(name = "notes", length = 2000)
    @Schema(description = "Примечания")
    private String notes;

    @Column(name = "photo", length = 255)
    @Schema(description = "Путь к фото сварщика")
    private String photo;

    @OneToMany(mappedBy = "welder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Schema(description = "RFID пропуска сварщика")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<RfidPass> rfidPasses;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "welder_welding_machine",
            joinColumns = @JoinColumn(name = "welder_id"),
            inverseJoinColumns = @JoinColumn(name = "welding_machine_id")
    )
    @JsonIgnoreProperties({"welders", "weldingMachineStates", "maintenances", "weldingLimitPrograms", "weldingMachineType", "organizationUnit"})
    @Schema(description = "Связанные сварочные аппараты")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<WeldingMachine> weldingMachines = new ArrayList<>();

    public enum WelderStatus {
        ACTIVE("Активен"),
        INACTIVE("Неактивен"),
        ON_LEAVE("В отпуске"),
        DISMISSED("Уволен"),
        BLOCKED("Заблокирован");

        private final String displayName;

        WelderStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
