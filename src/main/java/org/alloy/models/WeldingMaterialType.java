package org.alloy.models;

public enum WeldingMaterialType {
    WIRE(1),           // проволока
    GAS(2),
    FLUX(3),           // флюс
    ELECTRODE(4),
    WELDED_MATERIAL(5); // свариваемый материал

    private final int value;

    WeldingMaterialType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
