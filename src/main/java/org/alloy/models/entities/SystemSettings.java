package org.alloy.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SystemSettings")
@Data
@NoArgsConstructor
@Schema(description = "Системные настройки")
public class SystemSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Schema(description = "Уникальный идентификатор", example = "1")
    private Integer id;

    @Column(name = "Category", nullable = false)
    @Schema(description = "Категория настроек", example = "DATA_RETENTION")
    private String category;

    @Column(name = "SettingKey", nullable = false)
    @Schema(description = "Ключ настройки", example = "welding_data_days")
    private String settingKey;

    @Column(name = "SettingValue", nullable = false)
    @Schema(description = "Значение настройки", example = "365")
    private String settingValue;

    @Column(name = "Description")
    @Schema(description = "Описание настройки", example = "Время хранения данных сварки в днях")
    private String description;

    @Column(name = "DataType", nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "Тип данных", example = "INTEGER")
    private DataType dataType;

    @Column(name = "IsActive", nullable = false)
    @Schema(description = "Активна ли настройка", example = "true")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "DateCreated", nullable = false, updatable = false)
    @Schema(description = "Дата создания", example = "2024-01-15T10:30:00")
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "DateUpdated", nullable = false)
    @Schema(description = "Дата обновления", example = "2024-01-15T14:30:00")
    private LocalDateTime dateUpdated;

    public enum DataType {
        STRING("Строка"),
        INTEGER("Целое число"),
        DOUBLE("Дробное число"),
        BOOLEAN("Логическое значение"),
        JSON("JSON объект");

        private final String displayName;

        DataType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Category {
        DATA_RETENTION("Время хранения данных"),
        USER_INACTIVITY("Неактивность пользователей"),
        SYSTEM("Системные настройки"),
        NOTIFICATIONS("Уведомления"),
        BACKUP("Резервное копирование");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return "SystemSettings{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", settingKey='" + settingKey + '\'' +
                ", settingValue='" + settingValue + '\'' +
                ", dataType=" + dataType +
                ", isActive=" + isActive +
                '}';
    }
}
