package org.alloy.models;

public enum GeneralStatus {
    ACTIVE(1),
    INACTIVE(2),
    DELETED(4);

    private final int value;

    GeneralStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
