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
@Table(name = "NetworkEquipment")
@Data
@NoArgsConstructor
@Schema(description = "Сетевое оборудование системы мониторинга")
public class NetworkEquipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Schema(description = "Уникальный идентификатор", example = "1")
    private Integer id;

    @Column(name = "Name", nullable = false)
    @Schema(description = "Название оборудования", example = "Мониторинг-блок МС-1001")
    private String name;

    @Column(name = "Type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "Тип оборудования", example = "MONITORING_BLOCK")
    private EquipmentType type;

    @Column(name = "IPAddress", nullable = false)
    @Schema(description = "IP адрес", example = "192.168.1.100")
    private String ipAddress;

    @Column(name = "MACAddress", nullable = false)
    @Schema(description = "MAC адрес", example = "00:1B:44:11:3A:B7")
    private String macAddress;

    @Column(name = "Location")
    @Schema(description = "Расположение", example = "Цех №1, участок А")
    private String location;

    @Column(name = "Description")
    @Schema(description = "Описание", example = "Блок мониторинга сварочного оборудования")
    private String description;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    @Schema(description = "Статус оборудования", example = "Active")
    private GeneralStatus status;

    @Column(name = "LastSeen")
    @Schema(description = "Последняя активность", example = "2024-01-15T14:30:00")
    private LocalDateTime lastSeen;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    @Schema(description = "Дата создания", example = "2024-01-15T10:30:00")
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "DateUpdated", nullable = false)
    @Schema(description = "Дата обновления", example = "2024-01-15T14:30:00")
    private LocalDateTime dateUpdated;

    public enum EquipmentType {
        MONITORING_BLOCK("Блок мониторинга"),
        RADIO_RECEIVER("Радио-приемник"),
        ROUTER("Роутер"),
        HUB("Концентратор"),
        SWITCH("Коммутатор");

        private final String displayName;

        EquipmentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return "NetworkEquipment{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", ipAddress='" + ipAddress + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", location='" + location + '\'' +
                ", status=" + status +
                ", lastSeen=" + lastSeen +
                '}';
    }
}
