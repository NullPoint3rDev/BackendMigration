package org.alloy.services;

public class CorePacket {
    public long index;                 // uint32

    public int hours;                  // uint8
    public int minutes;                // uint8
    public int seconds;                // uint8
    public int date;                   // uint8

    public int month;                  // uint8
    public int year;                   // uint8
    /** Мгновенная скорость подачи проволоки (uint16 BE, сырьё: десятые м/мин). */
    public int instantWireFeedSpeedTenths;

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

    // New tail parameters (each is uint8)
    public int weldingMode;             // uint8 (modes_t)
    public int weldingMaterial;         // uint8 (materials_t)
    public int weldingGas;              // uint8 (gases_t)
    public int weldingWireDiameter;     // uint8 (diameters_t)
    public int burnerMode;              // uint8 (burnerMode_t)
    public int memoryCellNumber;        // uint8 (0 means not using memory cell)

    // Warnings: 6 bytes (3 x int16) immediately after memoryCellNumber
    public int warnings1;              // int16
    public int warnings2;              // int16
    public int warnings3;              // int16

    /** Время работы аппарата с момента включения (uint32, Big-Endian; единица — по прошивке, часто с или мс). */
    public long workTimeSincePowerOn;
    /** Время сварки с момента включения аппарата (uint32, Big-Endian). */
    public long weldingTimeSincePowerOn;

    /** Мгновенный расход газа (uint16, Big-Endian), сырьё: десятые л/мин — в хвосте пакета. */
    public int instantGasFlowLpm;
    /** Накопленный расход газа с включения (uint32, Big-Endian), сырьё: десятые литра. */
    public long gasConsumptionSincePowerOnLiters;
    /** true, если в пакете присутствуют поля 39–40 хвоста (газ). */
    public boolean hasExtendedGasMetrics;

    public double getDisplayCurrent() {
        return (weldingMachineState == 1) ? weldingCurrent : current;
    }

    public double getDisplayVoltage() {
        int raw = (weldingMachineState == 1) ? weldingVoltage : voltage;
        return raw / 10.0;
    }

    /** Мгновенный расход газа, л/мин (сырьё / 10). */
    public double getDisplayInstantGasFlowLpm() {
        int raw = hasExtendedGasMetrics ? instantGasFlowLpm : gasFlow;
        return raw / 10.0;
    }

    /** Мгновенная скорость подачи проволоки, м/мин (сырьё / 10). */
    public double getDisplayInstantWireFeedMpm() {
        return instantWireFeedSpeedTenths / 10.0;
    }

    /** Накопленный расход газа с включения, л (сырьё / 10). */
    public double getDisplayGasConsumptionSincePowerOnLiters() {
        return gasConsumptionSincePowerOnLiters / 10.0;
    }

    @Override
    public String toString() {
        return "CorePacket{" +
                "index=" + index + " (0x" + String.format("%08X", index) + ")" +
                ", time=" + String.format("%02d:%02d:%02d", hours, minutes, seconds) +
                ", date=" + String.format("%02d-%02d-%02d", year, month, date) +
                ", instantWireFeedSpeedTenths=" + instantWireFeedSpeedTenths +
                ", instantWireFeedMpm=" + getDisplayInstantWireFeedMpm() +
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
                ", weldingMode=" + weldingMode +
                ", weldingMaterial=" + weldingMaterial +
                ", weldingGas=" + weldingGas +
                ", weldingWireDiameter=" + weldingWireDiameter +
                ", burnerMode=" + burnerMode +
                ", memoryCellNumber=" + memoryCellNumber +
                ", warnings=[" + warnings1 + "," + warnings2 + "," + warnings3 + "]" +
                ", workTimeSincePowerOn=" + workTimeSincePowerOn +
                ", weldingTimeSincePowerOn=" + weldingTimeSincePowerOn +
                ", instantGasFlowLpm=" + instantGasFlowLpm +
                ", gasConsumptionSincePowerOnLiters=" + gasConsumptionSincePowerOnLiters +
                ", hasExtendedGasMetrics=" + hasExtendedGasMetrics +
                '}';
    }
}


