package org.alloy.models.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

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
    
    public enum WelderStatus {
        ACTIVE("Активен"),
        INACTIVE("Неактивен"),
        ON_LEAVE("В отпуске"),
        DISMISSED("Уволен");
        
        private final String displayName;
        
        WelderStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
