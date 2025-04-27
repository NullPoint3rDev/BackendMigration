package org.alloy.models.machine;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class MachineState {
    private MachineStatus status = MachineStatus.OFFLINE;
    private LocalDateTime lastUpdateTime;
    private Map<String, String> rawValues;
    private String barcode;

    // Current machine parameters
    private double current;
    private double voltage;
    private double power;
    private double gasFlow;
    private double temperature;
}
