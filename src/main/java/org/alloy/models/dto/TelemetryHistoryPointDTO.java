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
}

