package org.alloy.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceModelServiceDebugMacTest {

    private final DeviceModelService service = new DeviceModelService();

    @Test
    void isDebugMac_acceptsTwelveX() {
        assertTrue(DeviceModelService.isDebugMac("xxxxxxxxxxxx"));
        assertTrue(DeviceModelService.isDebugMac("XXXXXXXXXXXX"));
        assertTrue(DeviceModelService.isDebugMac("xx:xx:xx:xx:xx:xx"));
        assertFalse(DeviceModelService.isDebugMac("xxxxxxxxxxx")); // 11
        assertFalse(DeviceModelService.isDebugMac("E072A1D43F18"));
        assertFalse(DeviceModelService.isDebugMac(null));
    }

    @Test
    void normalizeMac_preservesDebugMac() {
        assertEquals(DeviceModelService.DEBUG_MAC, service.normalizeMac("xxxxxxxxxxxx"));
        assertEquals(DeviceModelService.DEBUG_MAC, service.normalizeMac("XX-XX-XX-XX-XX-XX"));
        assertEquals("E072A1D43F18", service.normalizeMac("e072a1d43f18"));
    }

    @Test
    void isValidMacFormat_acceptsDebugMac() {
        assertTrue(service.isValidMacFormat("xxxxxxxxxxxx"));
        assertTrue(service.isValidMacFormat("E072A1D43F18"));
        assertFalse(service.isValidMacFormat("zzzzzzzzzzzz"));
        assertFalse(service.isValidMacFormat("abc"));
    }
}
