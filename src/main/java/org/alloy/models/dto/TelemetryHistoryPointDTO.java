package org.alloy.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryHistoryPointDTO {
    private long ts;
    private Double current;
    private Double voltage;
    /** Установленный ток (Current в простое; null во время сварки). */
    private Double setCurrent;
    /** Установленное напряжение, В (в простое; null во время сварки). */
    private Double setVoltage;
    /** Мгновенный расход газа, л/мин (State.GasFlow). */
    private Double gasFlowLpm;
    private Double mainsVoltageA;
    private Double mainsVoltageB;
    private Double mainsVoltageC;
    private Double primaryCoilTemperature;
    private Double secondaryCoilTemperature;
    private Double chillerTemperatureIn;
    private Double chillerTemperatureOut;
    private String rfid;
    private String errorCode;
    private String status;
    /** Текст WeldingMachineState / «Состояние аппарата» (для дежурного режима на дорожке «Состояние»). */
    private String machineStateText;
    /** Факт сварки (дуга): текст «Сварка», статус Welding или газ+ток+напряжение на Core. */
    private Boolean isWelding;
}

