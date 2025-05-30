package org.alloy.services;

import org.alloy.models.configuration.PropertyCodes;
import org.alloy.models.configuration.WeldingMachineTypeConfiguration;
import org.alloy.models.machine.ProgramControls.ProgramControlItemValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MachineControlMessageBuilderService {
    private final WeldingMachineTypeConfiguration configuration;

    public Map<String, String> buildControlMessage(Map<String, ProgramControlItemValue> values) {
        Map<String, String> message = new HashMap<>();

        // Add all control values
        values.forEach((propertyCode, value) -> {
            if (value != null && value.getValue() != null) {
                message.put(propertyCode, value.getValue());
            }
        });

        // Calculate CRC8 if needed
        if (configuration.getOutbound().getBody() != null) {
            boolean hasCrc = false;
            for (var property : configuration.getOutbound().getBody()) {
                if (PropertyCodes.CRC8.equals(property.getPropertyCode())) {
                    hasCrc = true;
                    break;
                }
            }

            if (hasCrc) {
                String crc = calculateCRC8(message);
                message.put(PropertyCodes.CRC8, crc);
            }
        }

        return message;
    }

    private String calculateCRC8(Map<String, String> message) {
        // Implementation of CRC8 calculation
        // This is a placeholder - implement the actual CRC8 algorithm based on requirements
        byte crc = 0;
        for (Map.Entry<String, String> entry : message.entrySet()) {
            if (!PropertyCodes.CRC8.equals(entry.getKey())) {
                byte[] bytes = (entry.getKey() + entry.getValue()).getBytes();
                for (byte b : bytes) {
                    crc ^= b;
                    for (int i = 0; i < 8; i++) {
                        if ((crc & 0x80) != 0) {
                            crc = (byte) ((crc << 1) ^ 0x07);
                        } else {
                            crc <<= 1;
                        }
                    }
                }
            }
        }
        return String.format("%02X", crc);
    }
}
