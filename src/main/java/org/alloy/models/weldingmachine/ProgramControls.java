package org.alloy.models.weldingmachine;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ProgramControls {
    private String name;
    private int weldingMachineTypeId;
    private List<ProgramControlItem> items = new ArrayList<>();
    private int weldingMaterialId;
    private int gasWeldingMaterialId;
    private Double normGasFlow;
    private Double gasFlowNotifyTreshold;
}

@Data
@NoArgsConstructor
class ProgramControlItem {
    private String id;
    private String label;
    private ProgramControlItemType type;
    private Map<String, String> options = new HashMap<>();
    private double rangeMinValue;
    private double rangeMaxValue;
    private double step;
    private String visibilityItemId;
    private String visibilityItemValue;
}

@Data
@NoArgsConstructor
class ProgramControlItemValue {
    private String id;
    private String value;
    private double minValue;
    private double maxValue;
}

enum ProgramControlItemType {
    OPTION(1),
    MULTIPLE_OPTION(2),
    NUMERIC_RANGE(3),
    NUMBER(4);

    private final int value;

    ProgramControlItemType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
