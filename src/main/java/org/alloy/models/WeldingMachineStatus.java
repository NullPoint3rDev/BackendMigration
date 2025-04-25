package org.alloy.models;

public enum WeldingMachineStatus {
    OFF(0),
    READY(1),
    SERVICE(2),
    WORKING(3),
    ERROR(4);

    private final int value;

    WeldingMachineStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
