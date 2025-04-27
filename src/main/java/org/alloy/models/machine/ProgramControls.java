package org.alloy.models.machine;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ProgramControls {
    private String name;
    private int weldingMachineTypeId;
    private List<ProgramControlItem> items;
    private int weldingMaterialId;
    private int gasWeldingMaterialId;
    private Double normGasFlow;
    private Double gasFlowNotifyThreshold;

    @Data
    public static class ProgramControlItem {
        private String id;
        private String label;
        private ProgramControlItemType type;
        private Map<String, String> options;
        private double rangeMinValue;
        private double rangeMaxValue;
        private double step;
        private String visibilityItemId;
        private String visibilityItemValue;
    }

    @Data
    public static class ProgramControlItemValue {
        private String id;
        private String value;
        private double minValue;
        private double maxValue;
    }

    public enum ProgramControlItemType {
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
}
