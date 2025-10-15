package org.alloy.services;

import org.alloy.controllers.DeviceTestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceMessageHistoryService {

    @Autowired
    private DeviceTestController deviceTestController;

    /**
     * Добавить сообщение в историю
     */
    public void addMessage(String mac, String data, String type) {
        if (deviceTestController != null) {
            deviceTestController.addDeviceMessage(mac, data, type);
        }
    }
}
