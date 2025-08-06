package org.alloy.models.weldingmachine;

import org.alloy.models.WeldingMachineStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class StateSummary {
    private long weldingMachineStateId;
    private LocalDateTime dateCreated;
    private LocalDateTime lastDatetimeUpdate;
    private WeldingMachineStatus status;
    private String control;
    private String controlState;
    private long stateDurationMs;
    private String errorCode;
    private Integer weldingMaterialId;
    private Integer gasWeldingMaterialId;
    private Integer organizationUnitId;
    private boolean limitsExceeded;
    private Integer weldingLimitProgramId;
    private String weldingLimitProgramName;
    private Double normGasFlow;
    private String md5;
    private LocalDateTime localServerPacketDatetime;
    private Map<String, StateSummaryPropertyValue> properties = new HashMap<>();
    private boolean isOfflineData = false;
    private Double gasFlowNotifyTreshold;
}
