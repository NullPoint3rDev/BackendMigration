package org.alloy.services;

import org.alloy.models.MacRegistryStatus;
import org.alloy.models.entities.MacAddressRegistry;
import org.alloy.repositories.MacAddressRegistryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MacAddressRegistryServiceTest {

    @Mock
    private MacAddressRegistryRepository registryRepository;

    @Mock
    private org.alloy.repositories.MacEquipmentTypeRepository equipmentTypeRepository;

    @Mock
    private DeviceModelService deviceModelService;

    @Mock
    private ArchiveStyleTcpListener archiveStyleTcpListener;

    @InjectMocks
    private MacAddressRegistryService service;

    @Test
    void recordPacket_incrementsSessionAfterGap() {
        MacAddressRegistry row = new MacAddressRegistry();
        row.setMac("AABBCCDDEEFF");
        row.setStatus(MacRegistryStatus.WAITING);
        row.setSessionCount(1L);
        row.setLastPacketAt(LocalDateTime.now().minusMinutes(11));

        when(deviceModelService.normalizeMac("AA:BB:CC:DD:EE:FF")).thenReturn("AABBCCDDEEFF");
        when(deviceModelService.isValidMacFormat("AABBCCDDEEFF")).thenReturn(true);
        when(registryRepository.findByMac("AABBCCDDEEFF")).thenReturn(Optional.of(row));

        service.recordPacket("AA:BB:CC:DD:EE:FF");

        assertEquals(2L, row.getSessionCount());
        verify(registryRepository).save(row);
    }

    @Test
    void recordPacket_doesNotIncrementWithinTenMinutes() {
        MacAddressRegistry row = new MacAddressRegistry();
        row.setMac("AABBCCDDEEFF");
        row.setStatus(MacRegistryStatus.ACTIVE);
        row.setSessionCount(5L);
        row.setLastPacketAt(LocalDateTime.now().minusMinutes(3));

        when(deviceModelService.normalizeMac("AABBCCDDEEFF")).thenReturn("AABBCCDDEEFF");
        when(deviceModelService.isValidMacFormat("AABBCCDDEEFF")).thenReturn(true);
        when(registryRepository.findByMac("AABBCCDDEEFF")).thenReturn(Optional.of(row));

        service.recordPacket("AABBCCDDEEFF");

        assertEquals(5L, row.getSessionCount());
        verify(registryRepository).save(any(MacAddressRegistry.class));
    }
}
