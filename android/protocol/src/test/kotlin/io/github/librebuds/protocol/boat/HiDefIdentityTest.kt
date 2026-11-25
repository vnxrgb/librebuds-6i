package io.github.librebuds.protocol.boat

import org.junit.Assert.*
import org.junit.Test

class HiDefIdentityTest {
    @Test fun exactHiDefIdentity() {
        assertTrue(HiDefIdentity.matches("boAt Airdopes 800 HiDef"))
        assertTrue(HiDefIdentity.matches("Airdopes 800 Hi-Def"))
        assertFalse(HiDefIdentity.matches("Airdopes 800"))
        assertFalse(HiDefIdentity.matches("Airdopes 800D"))
        assertFalse(HiDefIdentity.matches("FreeBuds 6i"))
        assertFalse(HiDefIdentity.matches(null))
    }

    @Test fun besOfflineFrameChecksLengthAndHeader() {
        assertEquals(BesFrame(0x15, listOf(2, 1)), BesFrameDecoder.decode(byteArrayOf(8, 0x63, 3, 0, 0x15, 2, 1)))
        assertNull(BesFrameDecoder.decode(byteArrayOf(8, 0x63, 4, 0, 0x15, 2, 1)))
        assertNull(BesFrameDecoder.decode(byteArrayOf(9, 0x63, 1, 0, 0x15)))
    }
}
