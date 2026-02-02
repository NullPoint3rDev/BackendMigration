package org.alloy.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.alloy.models.entities.Welder;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "DTO для создания/обновления сварщика")
public class WelderDTO {

    @Schema(description = "ФИО сварщика")
    private String name;

    @Schema(description = "Статус сварщика")
    private Welder.WelderStatus status;

    @Schema(description = "Подразделение")
    private String department;

    @Schema(description = "Должность")
    private String position;

    @Schema(description = "Разряд")
    private String grade;

    @Schema(description = "Табельный номер")
    private String employeeId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Дата приема на работу")
    private LocalDate hireDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Дата рождения")
    private LocalDate birthDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Дата аттестации")
    private LocalDate certificationDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Дата следующей аттестации")
    private LocalDate nextCertificationDate;

    @Schema(description = "Номер телефона")
    private String phone;

    @Schema(description = "Адрес")
    private String address;

    @Schema(description = "Сведения об образовании")
    private String education;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Примечания")
    private String notes;

    @Schema(description = "Путь к фото сварщика")
    private String photo;

    @Schema(description = "Список RFID кодов пропусков")
    private List<String> rfidCodes;

    @Schema(description = "Список ID связанных сварочных аппаратов")
    private List<Integer> machineIds;
}

