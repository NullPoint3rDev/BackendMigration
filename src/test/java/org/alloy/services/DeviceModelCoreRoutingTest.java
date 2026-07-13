package org.alloy.services;

import org.alloy.models.DeviceModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceModelCoreRoutingTest {

    @Test
    void getByMac_recognizesProductionCoreMacs() {
        assertEquals(DeviceModel.CORE, DeviceModel.getByMac("E098060B22D2"));
        assertEquals(DeviceModel.CORE, DeviceModel.getByMac("C82B9620E506"));
    }

    @Test
    void isCorePacketFormat_distinguishesCoreFromArchiveBlock() {
        String coreLike = ":E098060B22D2;" + "AB".repeat(60);
        String archiveLike = ":8CAAB50C4254;" + "00".repeat(38);
        assertTrue(DeviceModelService.isCorePacketFormat(coreLike));
        assertFalse(DeviceModelService.isCorePacketFormat(archiveLike));
    }
}
