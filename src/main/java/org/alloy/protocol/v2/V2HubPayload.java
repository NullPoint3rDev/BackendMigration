package org.alloy.protocol.v2;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hub «Полезные данные» без type/len/crc — тело поля {@code данные} в v2 0x02/0x07.
 * Все multi-byte — little-endian. fixed×10 уже поделены на 10.
 */
public final class V2HubPayload {
    public int packetIndex;
    public long unixTime;
    public int machineState;
    public int jobNumber;
    public int weldMode;
    public int wireMaterial;
    public int weldGas;
    public int wireDiameter;
    public int torchMode;
    public int setCurrentA;
    public double setWireSpeedMPerMin;
    public double setVoltageV;
    public int memoryCell;
    public int setInductance;
    public int actualCurrentA;
    public double actualVoltageV;
    public long passNumber;
    public int mainsPhaseAV;
    public int mainsPhaseBV;
    public int mainsPhaseCV;
    public double tempBvoInC;
    public double tempBvoOutC;
    public double tempRadiatorC;
    public double tempTerminalPlusC;
    public double tempTerminalMinusC;
    public double gasFlowLPerMin;
    public long gasTotalL;
    public long wireTotalM;
    public long uptimeSec;
    public long weldTimeSec;
    public int errors1;
    public int errors2;
    public int errors3;
    public int warnings1;
    public int warnings2;
    public int warnings3;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("packetIndex", packetIndex);
        m.put("unixTime", unixTime);
        m.put("machineState", machineState);
        m.put("jobNumber", jobNumber);
        m.put("weldMode", weldMode);
        m.put("wireMaterial", wireMaterial);
        m.put("weldGas", weldGas);
        m.put("wireDiameter", wireDiameter);
        m.put("torchMode", torchMode);
        m.put("setCurrentA", setCurrentA);
        m.put("setWireSpeedMPerMin", setWireSpeedMPerMin);
        m.put("setVoltageV", setVoltageV);
        m.put("memoryCell", memoryCell);
        m.put("setInductance", setInductance);
        m.put("actualCurrentA", actualCurrentA);
        m.put("actualVoltageV", actualVoltageV);
        m.put("passNumber", passNumber);
        m.put("mainsPhaseAV", mainsPhaseAV);
        m.put("mainsPhaseBV", mainsPhaseBV);
        m.put("mainsPhaseCV", mainsPhaseCV);
        m.put("tempBvoInC", tempBvoInC);
        m.put("tempBvoOutC", tempBvoOutC);
        m.put("tempRadiatorC", tempRadiatorC);
        m.put("tempTerminalPlusC", tempTerminalPlusC);
        m.put("tempTerminalMinusC", tempTerminalMinusC);
        m.put("gasFlowLPerMin", gasFlowLPerMin);
        m.put("gasTotalL", gasTotalL);
        m.put("wireTotalM", wireTotalM);
        m.put("uptimeSec", uptimeSec);
        m.put("weldTimeSec", weldTimeSec);
        m.put("errors1", errors1);
        m.put("errors2", errors2);
        m.put("errors3", errors3);
        m.put("warnings1", warnings1);
        m.put("warnings2", warnings2);
        m.put("warnings3", warnings3);
        return m;
    }
}
