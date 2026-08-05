package org.alloy.protocol.v2;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.alloy.models.WeldingMachineStatus;
import org.alloy.models.weldingmachine.StateSummary;
import org.alloy.models.weldingmachine.StateSummaryPropertyValue;

/**
 * Hub payload → StateSummary для записи в БД (без ASCII/Core-парсера).
 */
public final class V2HubStateMapper {
    private V2HubStateMapper() {}

    public static StateSummary toStateSummary(V2HubPayload hub, boolean historyRecord) {
        if (hub == null) {
            return null;
        }
        StateSummary s = new StateSummary();
        s.setStatus(mapStatus(hub.machineState));
        s.setControl("HUB");
        s.setControlState(String.valueOf(hub.machineState));
        s.setStateDurationMs(0);
        s.setOfflineData(historyRecord);
        s.setErrorCode(hub.errors1 != 0 ? ("E1:" + hub.errors1) : null);

        LocalDateTime ts = unixToUtc(hub.unixTime);
        s.setDateCreated(ts);
        s.setLastDatetimeUpdate(ts);
        s.setLocalServerPacketDatetime(LocalDateTime.now(ZoneOffset.UTC));

        Map<String, StateSummaryPropertyValue> props = new HashMap<>();
        add(props, "State.I", String.valueOf(hub.actualCurrentA), "number");
        add(props, "State.U", String.valueOf(hub.actualVoltageV), "number");
        add(props, "State.I.set", String.valueOf(hub.setCurrentA), "number");
        add(props, "State.U.set", String.valueOf(hub.setVoltageV), "number");
        add(props, "State.WFS", String.valueOf(hub.setWireSpeedMPerMin), "number");
        add(props, "Hub.Job", String.valueOf(hub.jobNumber), "number");
        add(props, "Hub.Mode", String.valueOf(hub.weldMode), "number");
        add(props, "Hub.PacketIndex", String.valueOf(hub.packetIndex), "number");
        s.setProperties(props);
        return s;
    }

    /** Hub machineState → WeldingMachineStatus. */
    static WeldingMachineStatus mapStatus(int machineState) {
        return switch (machineState) {
            case 0 -> WeldingMachineStatus.Online;
            case 1 -> WeldingMachineStatus.Welding;
            case 2 -> WeldingMachineStatus.Error;
            case 3, 4 -> WeldingMachineStatus.Idle;
            case 5 -> WeldingMachineStatus.Offline;
            default -> WeldingMachineStatus.Online;
        };
    }

    private static LocalDateTime unixToUtc(long unixSec) {
        if (unixSec <= 0) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixSec), ZoneOffset.UTC);
    }

    private static void add(
            Map<String, StateSummaryPropertyValue> props,
            String code,
            String value,
            String type) {
        StateSummaryPropertyValue p = new StateSummaryPropertyValue();
        p.setPropertyCode(code);
        p.setValue(value);
        p.setPropertyType(type);
        p.setRawValue(value);
        props.put(code, p);
    }
}
