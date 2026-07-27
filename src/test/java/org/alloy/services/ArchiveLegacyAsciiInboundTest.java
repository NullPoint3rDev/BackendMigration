package org.alloy.services;

import org.alloy.metrics.WeldingMetrics;
import org.alloy.repositories.WeldingMachineRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Legacy ASCII {@code :MAC;HEX} → {@link ArchiveStyleTcpListener} → queue →
 * {@link ArchiveStylePacketParser} (+ {@link CorePacketParser} на том же кадре).
 * Без Spring / реального ServerSocket.
 */
@ExtendWith(MockitoExtension.class)
class ArchiveLegacyAsciiInboundTest {

    private static final String CORE_MAC = "E09806083396";
    private static final String ARCHIVE_MAC = "8CAAB50C4254";
    /** Короткий archive-блок (~76 nibbles) — не core format. */
    private static final String ARCHIVE_FRAME = ":" + ARCHIVE_MAC + ";" + "00".repeat(38);
    /** Длинный hex — core WTINFO format (≥100 nibbles). */
    private static final String CORE_FRAME = ":" + CORE_MAC + ";" + "AB".repeat(60);

    @Mock
    private WeldingMachineRepository weldingMachineRepository;
    @Mock
    private WeldingDeviceManagerService deviceManager;
    @Mock
    private WeldingMetrics weldingMetrics;
    @Mock
    private DeviceLivenessRegistry deviceLivenessRegistry;
    @Mock
    private DeviceModelService deviceModelService;
    @Mock
    private MacAddressRegistryService macAddressRegistryService;

    @BeforeEach
    @AfterEach
    void clearQueue() {
        ArchiveIncomingPacketsQueue.clear();
    }

    @Test
    void queue_enqueueDequeue_roundTrip() {
        ArchivePacket in = new ArchivePacket("127.0.0.1", CORE_MAC, CORE_FRAME);
        ArchiveIncomingPacketsQueue.enqueue(in);

        assertEquals(1, ArchiveIncomingPacketsQueue.size());
        ArchivePacket out = ArchiveIncomingPacketsQueue.tryDequeue();
        assertNotNull(out);
        assertEquals(CORE_MAC, out.getMac());
        assertEquals(CORE_FRAME, out.getData());
        assertTrue(ArchiveIncomingPacketsQueue.isEmpty());
        assertNull(ArchiveIncomingPacketsQueue.tryDequeue());
    }

    @Test
    void packetParser_coreRoute_callsDeviceManagerWithoutArchiveParse() throws Exception {
        ArchiveStylePacketParser parser = newParser();
        when(deviceModelService.shouldUseCoreParser(CORE_MAC, CORE_FRAME)).thenReturn(true);

        ArchivePacket packet = new ArchivePacket("127.0.0.1", CORE_MAC, CORE_FRAME);
        parser.processPacket(packet);

        verify(deviceManager).processDeviceData(CORE_FRAME, CORE_MAC);
        assertNotNull(CorePacketParser.parse(CORE_FRAME));
    }

    @Test
    void packetParser_archiveRoute_stillForwardsRawFrameToDeviceManager() throws Exception {
        ArchiveStylePacketParser parser = newParser();
        when(deviceModelService.shouldUseCoreParser(ARCHIVE_MAC, ARCHIVE_FRAME)).thenReturn(false);

        parser.processPacket(new ArchivePacket("127.0.0.1", ARCHIVE_MAC, ARCHIVE_FRAME));

        verify(deviceManager).processDeviceData(ARCHIVE_FRAME, ARCHIVE_MAC);
        // короткий archive-блок не core-format; CorePacketParser всё равно что-то вернёт по hex
        assertFalse(DeviceModelService.isCorePacketFormat(ARCHIVE_FRAME));
    }

    @Test
    void packetParser_nullPacket_noop() throws Exception {
        ArchiveStylePacketParser parser = newParser();
        parser.processPacket(null);
        parser.processPacket(new ArchivePacket());
        verify(deviceManager, never()).processDeviceData(anyString(), anyString());
    }

    @Test
    void tcpListener_asciiFrame_enqueuesAllowedMac() throws Exception {
        ArchiveStyleTcpListener listener = wiredListener();
        when(macAddressRegistryService.isAllowedForTcp(CORE_MAC)).thenReturn(true);

        invokeHandleClient(listener, CORE_FRAME);

        ArchivePacket queued = ArchiveIncomingPacketsQueue.tryDequeue();
        assertNotNull(queued);
        assertEquals(CORE_MAC, queued.getMac());
        assertEquals(CORE_FRAME, queued.getData());
        assertEquals("127.0.0.1", queued.getIp());

        verify(deviceLivenessRegistry).markSeen(CORE_MAC);
        verify(macAddressRegistryService).recordPacket(CORE_MAC);
        verify(deviceManager).touchInboundTelemetry(CORE_MAC);
        verify(weldingMetrics).tcpConnectionOpened();
        verify(weldingMetrics).tcpConnectionClosed();
    }

    @Test
    void tcpListener_asciiFrame_rejectsUnknownMac() throws Exception {
        ArchiveStyleTcpListener listener = wiredListener();
        when(macAddressRegistryService.isAllowedForTcp(ARCHIVE_MAC)).thenReturn(false);
        when(deviceModelService.isCoreMacInConfig(ARCHIVE_MAC)).thenReturn(false);

        invokeHandleClient(listener, ARCHIVE_FRAME);

        assertTrue(ArchiveIncomingPacketsQueue.isEmpty());
        verify(weldingMetrics).recordUnknownMac();
        verify(macAddressRegistryService, never()).recordPacket(anyString());
        verify(deviceLivenessRegistry).markSeen(ARCHIVE_MAC);
    }

    @Test
    void tcpListener_toQueue_toParser_coreEndToEnd() throws Exception {
        ArchiveStyleTcpListener listener = wiredListener();
        when(macAddressRegistryService.isAllowedForTcp(CORE_MAC)).thenReturn(true);
        when(deviceModelService.shouldUseCoreParser(eq(CORE_MAC), eq(CORE_FRAME))).thenReturn(true);

        invokeHandleClient(listener, CORE_FRAME);

        ArchivePacket queued = ArchiveIncomingPacketsQueue.tryDequeue();
        assertNotNull(queued);

        ArchiveStylePacketParser parser = newParser();
        parser.processPacket(queued);

        verify(deviceManager).processDeviceData(CORE_FRAME, CORE_MAC);
        CorePacket parsed = CorePacketParser.parse(queued.getData());
        assertNotNull(parsed);
        // AB repeated → index = 0xABABABAB
        assertEquals(0xABABABABL, parsed.index);
    }

    @Test
    void tcpListener_connectBurst_enqueuesAndAsciiPollMode() throws Exception {
        ArchiveStyleTcpListener listener = wiredListener();
        when(macAddressRegistryService.isAllowedForTcp(CORE_MAC)).thenReturn(true);

        String frame2 = ":C82B9620E506;00001CAF0B1A031B071A00460003000000000000003500DC00F500000000000000000190019001910108010A0000000000000000000000000000000004010004000000000000000000001C5E00000000000000000000F7";
        String connectBlob = "CONNECT Core4Machine:" + CORE_FRAME.substring(1) + "\n" + frame2;

        invokeHandleClient(listener, connectBlob);

        ArchivePacket queued = ArchiveIncomingPacketsQueue.tryDequeue();
        assertNotNull(queued);
        assertEquals(CORE_MAC, queued.getMac());
        assertTrue(queued.getData().startsWith("CONNECT "));

        String picked = CoreAsciiFrameExtractor.pickLastParseableFrame(queued.getData());
        assertEquals(frame2, picked);
        assertNotNull(CorePacketParser.parse(picked));
    }

    private ArchiveStylePacketParser newParser() throws Exception {
        ArchiveStylePacketParser parser = new ArchiveStylePacketParser();
        setField(parser, "deviceModelService", deviceModelService);
        setField(parser, "deviceManager", deviceManager);
        setField(parser, "debugMode", false);
        return parser;
    }

    private ArchiveStyleTcpListener wiredListener() throws Exception {
        ArchiveStyleTcpListener listener = new ArchiveStyleTcpListener(weldingMachineRepository);
        setField(listener, "deviceManager", deviceManager);
        setField(listener, "weldingMetrics", weldingMetrics);
        setField(listener, "deviceLivenessRegistry", deviceLivenessRegistry);
        setField(listener, "deviceModelService", deviceModelService);
        setField(listener, "macAddressRegistryService", macAddressRegistryService);
        setField(listener, "v2ProtocolService", null);
        setField(listener, "coreOutboundService", null);
        setField(listener, "messagingTemplate", null);
        setField(listener, "archiveAllowedMacsConfig", "");
        setField(listener, "running", true);
        return listener;
    }

    /**
     * Один ASCII-кадр в сокет, затем стоп {@code running}, чтобы не ждать 30s idle-timeout.
     */
    private void invokeHandleClient(ArchiveStyleTcpListener listener, String frame) throws Exception {
        byte[] bytes = frame.getBytes(StandardCharsets.US_ASCII);
        AtomicBoolean stopArmed = new AtomicBoolean(false);

        InputStream in = new InputStream() {
            private int pos;

            @Override
            public int read() {
                return pos < bytes.length ? (bytes[pos++] & 0xFF) : -1;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (pos >= bytes.length) {
                    return -1;
                }
                int n = Math.min(len, bytes.length - pos);
                System.arraycopy(bytes, pos, b, off, n);
                pos += n;
                return n;
            }

            @Override
            public int available() {
                // после кадра asciiPollMode спит в цикле — гасим running
                if (pos >= bytes.length && stopArmed.compareAndSet(false, true)) {
                    try {
                        setField(listener, "running", false);
                    } catch (Exception ignored) {
                    }
                }
                return Math.max(0, bytes.length - pos);
            }
        };

        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(in);
        when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(socket.getInetAddress()).thenReturn(InetAddress.getByName("127.0.0.1"));
        when(socket.isClosed()).thenReturn(false);

        Method m = ArchiveStyleTcpListener.class.getDeclaredMethod("handleClientConnection", Socket.class);
        m.setAccessible(true);
        m.invoke(listener, socket);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
