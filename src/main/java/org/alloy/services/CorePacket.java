package org.alloy.services;

public class CorePacket {
    public long index;                 // uint32

    public int hours;                  // uint8
    public int minutes;                // uint8
    public int seconds;                // uint8
    public int date;                   // uint8

    public int month;                  // uint8
    public int year;                   // uint8
    public int reserve;                // uint16

    public int flags;                  // int8
    public int weldingMachineState;    // int8 (0=idle,1=welding?)
    public int gasFlow;                // int16

    public int weldingCurrent;         // int16
    public int weldingVoltage;         // int16 (display = /10)

    public int jobNumber;              // int16
    public int current;                // int16

    public int voltage;                // int16 (display = /10)
    public int inductance;             // int16

    public int errors1;                // int16
    public int errors2;                // int16

    public int errors3;                // int16
    public int voltagePhaseA;          // int16

    public int voltagePhaseB;          // int16
    public int voltagePhaseC;          // int16

    public int chillerTemperature1;    // int16
    public int chillerTemperature2;    // int16

    public int primaryCoilTemperature;   // int16
    public int secondaryCoilTemperature; // int16

    public long wireIndex;             // uint32

    public long rfidData;               // uint64

    public double getDisplayCurrent() {
        return (weldingMachineState == 1) ? weldingCurrent : current;
    }

    public double getDisplayVoltage() {
        int raw = (weldingMachineState == 1) ? weldingVoltage : voltage;
        return raw / 10.0;
    }

    @Override
    public String toString() {
        return "CorePacket{" +
                "index=" + index + " (0x" + String.format("%08X", index) + ")" +
                ", time=" + String.format("%02d:%02d:%02d", hours, minutes, seconds) +
                ", date=" + String.format("%02d-%02d-%02d", year, month, date) +
                ", flags=" + flags +
                ", weldingMachineState=" + weldingMachineState +
                ", gasFlow=" + gasFlow +
                ", weldingCurrent=" + weldingCurrent +
                ", weldingVoltage=" + (weldingVoltage/10.0) +
                ", jobNumber=" + jobNumber +
                ", current=" + current +
                ", voltage=" + (voltage/10.0) +
                ", inductance=" + inductance +
                ", errors=[" + errors1 + "," + errors2 + "," + errors3 + "]" +
                ", phases=[" + voltagePhaseA + "," + voltagePhaseB + "," + voltagePhaseC + "]" +
                ", chiller=[" + chillerTemperature1 + "," + chillerTemperature2 + "]" +
                ", coils=[" + primaryCoilTemperature + "," + secondaryCoilTemperature + "]" +
                ", wireIndex=" + wireIndex +
                ", rfidData=" + rfidData + " (0x" + String.format("%016X", rfidData) + ")" +
                ", displayCurrent=" + getDisplayCurrent() +
                ", displayVoltage=" + getDisplayVoltage() +
                '}';
    }
}


