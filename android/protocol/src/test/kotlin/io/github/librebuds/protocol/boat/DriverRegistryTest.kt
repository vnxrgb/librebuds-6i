package io.github.librebuds.protocol.boat

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DriverRegistryTest {
    @Test fun unknownHiDefNeverAuthorizesVendorWrites() {
        assertFalse(DriverRegistry.allowsWuqi("boAt Airdopes 800 HiDef", listOf(BoatDetection.wuqiService.toString())))
        assertTrue(DriverRegistry.candidates("boAt Airdopes 800 HiDef", emptyList()).isEmpty())
        assertTrue(DriverRegistry.allowsWuqi("Nirvana Ion ANC", listOf(BoatDetection.wuqiService.toString())))
    }

    @Test fun genericSppIsNotChipsetEvidence() {
        assertTrue(DriverRegistry.candidates("Unknown", listOf("00001101-0000-1000-8000-00805f9b34fb")).isEmpty())
    }

    @Test fun decodesOnlyWellFormedCaptures() {
        assertEquals(CapturedFrame(0x27, listOf(1), 2), CaptureDecoders.bluetrum(byteArrayOf(0, 0x27, 2, 0, 1, 1)))
        assertNull(CaptureDecoders.bluetrum(byteArrayOf(0, 0x27, 2, 1, 0)))
        assertEquals(CapturedFrame(0x0321, listOf(1), 0x5B), CaptureDecoders.airoha(byteArrayOf(5, 0x5B, 3, 0, 0x21, 3, 1)))
        assertNull(CaptureDecoders.airoha(byteArrayOf(5, 0x5B, 4, 0, 0x21, 3, 1)))
        assertEquals(CapturedFrame(0xF9, listOf(0)), CaptureDecoders.ceva(byteArrayOf(1, 0xE0.toByte(), 0xFC.toByte(), 2, 0xF9.toByte(), 0)))
        assertNull(CaptureDecoders.standardBattery(byteArrayOf(101)))
        assertEquals(0, CaptureDecoders.standardBattery(byteArrayOf(0)))
    }
}
