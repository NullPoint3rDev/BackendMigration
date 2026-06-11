package org.alloy.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class WeldSegmentDTO {

    // Русские заголовки в JSON для фронтенда, порядок полей сохранится как объявлен
    @JsonProperty("Средний Ток")
    private BigDecimal averageCurrent; // А, округление до 1 знака делаем при расчёте

    @JsonProperty("Среднее напряжение")
    private BigDecimal averageVoltage; // В, округление до 1 знака

    @JsonProperty("Начало шва")
    private LocalDateTime startTime; // серверная таймзона

    @JsonProperty("Длительность шва (с)")
    private BigDecimal durationSeconds; // сек, 1 знак после запятой

    /** Для материализованного кэша швов (не отдаётся в JSON отчёта). */
    @JsonIgnore
    private Long startStateId;

    @JsonIgnore
    private Long endStateId;
}


