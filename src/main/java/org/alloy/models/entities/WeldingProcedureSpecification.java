package org.alloy.models.entities;

import org.alloy.models.GeneralStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "WeldingProcedureSpecification")
@Data
@NoArgsConstructor
@Schema(description = "Технологическая карта сварки (WPS)")
public class WeldingProcedureSpecification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Schema(description = "Уникальный идентификатор", example = "1")
    private Integer id;

    @Column(name = "Name", nullable = false)
    @Schema(description = "Название WPS", example = "WPS-001")
    private String name;

    @Column(name = "Description")
    @Schema(description = "Описание", example = "Сварка низкоуглеродистой стали")
    private String description;

    @Column(name = "WeldingMethod", nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "Метод сварки", example = "MIG")
    private WeldingMethod weldingMethod;

    @Column(name = "MaterialType", nullable = false)
    @Schema(description = "Тип материала", example = "Ст3")
    private String materialType;

    @Column(name = "Thickness", nullable = false)
    @Schema(description = "Толщина", example = "3-8 мм")
    private String thickness;

    @Column(name = "CurrentMin", nullable = false)
    @Schema(description = "Минимальный ток (А)", example = "120")
    private Integer currentMin;

    @Column(name = "CurrentMax", nullable = false)
    @Schema(description = "Максимальный ток (А)", example = "180")
    private Integer currentMax;

    @Column(name = "VoltageMin", nullable = false)
    @Schema(description = "Минимальное напряжение (В)", example = "18")
    private Integer voltageMin;

    @Column(name = "VoltageMax", nullable = false)
    @Schema(description = "Максимальное напряжение (В)", example = "22")
    private Integer voltageMax;

    @Column(name = "FeedRate", nullable = false)
    @Schema(description = "Скорость подачи", example = "4-6 м/мин")
    private String feedRate;

    @Column(name = "GasConsumption", nullable = false)
    @Schema(description = "Расход газа", example = "12-15 л/мин")
    private String gasConsumption;

    @Column(name = "GostStandard", nullable = false)
    @Schema(description = "ГОСТ", example = "ГОСТ 14771-76")
    private String gostStandard;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    @Schema(description = "Статус WPS", example = "Active")
    private GeneralStatus status;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    @Schema(description = "Дата создания", example = "2024-01-10T10:30:00")
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "DateUpdated", nullable = false)
    @Schema(description = "Дата обновления", example = "2024-01-15T14:30:00")
    private LocalDateTime dateUpdated;

    public enum WeldingMethod {
        MIG("MIG"),
        TIG("TIG"),
        MMA("MMA"),
        SAW("SAW");

        private final String displayName;

        WeldingMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return "WeldingProcedureSpecification{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", weldingMethod=" + weldingMethod +
                ", materialType='" + materialType + '\'' +
                ", thickness='" + thickness + '\'' +
                ", currentMin=" + currentMin +
                ", currentMax=" + currentMax +
                ", voltageMin=" + voltageMin +
                ", voltageMax=" + voltageMax +
                ", status=" + status +
                '}';
    }
}
