package io.github.librebuds.protocol.boat

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class WuqiProtocolTest {
    @Test fun exactModelAndServiceRequired() {
        val service = BoatDetection.wuqiService.toString()
        assertTrue(BoatDetection.isNirvanaIonAnc("boAt Nirvana Ion ANC", listOf(service)))
        assertFalse(BoatDetection.isNirvanaIonAnc("Nirvana Ion ANC", emptyList()))
        assertFalse(BoatDetection.isNirvanaIonAnc("Nirvana Ion", listOf(service)))
        assertFalse(BoatDetection.isNirvanaIonAnc("FreeBuds 6i", listOf(service)))
    }

    @Test fun onlyDocumentedReads() {
        assertEquals(setOf(0x0104, 0x0105, 0x010C, 0x0109), WuqiRead.values().map { it.code }.toSet())
        assertArrayEquals(byteArrayOf(8, 0xEE.toByte(), 0, 0, 0, 1, 5, 10, 0, 6), WuqiRead.BATTERY.request())
        assertArrayEquals(byteArrayOf(8, 0xEE.toByte(), 0, 0, 0, 1, 12, 10, 0, 13), WuqiRead.NOISE_MODE.request())
    }

    private fun reply(code: Int, vararg payload: Byte): ByteArray {
        val f = byteArrayOf(9, 0xFF.toByte(), 0, 0, 1, (code shr 8).toByte(), code.toByte(),
            (10 + payload.size).toByte(), 0) + payload + byteArrayOf(0)
        f[f.lastIndex] = f.dropLast(1).sumOf { it.toInt() and 255 }.toByte()
        return f
    }

    @Test fun fragmentedAndJoinedReplies() {
        val parser = WuqiReplyParser()
        val frame = reply(0x010C, 1)
        assertTrue(parser.feed(frame.copyOfRange(0, 5)).isEmpty())
        val result = parser.feed(frame.copyOfRange(5, frame.size) + reply(0x0105, 50))
        assertEquals(2, result.size)
        assertEquals(WuqiNoiseMode.ANC, result[0].noiseMode())
        assertNull(result[1].noiseMode())
    }

    @Test fun malformedAndUnknownRepliesIgnored() {
        val parser = WuqiReplyParser()
        val corrupt = reply(0x010C, 1).also { it[it.lastIndex] = 0 }
        val result = parser.feed(byteArrayOf(42) + corrupt + reply(0x0501) + reply(0x010C, 99))
        assertEquals(1, result.size)
        assertNull(result.single().noiseMode())
    }

    @Test fun batteryUnavailableIsNotZeroPercent() {
        assertEquals(WuqiBattery(75, null, null), WuqiReply(WuqiRead.BATTERY, byteArrayOf(75, 0, 120)).battery())
        assertNull(WuqiReply(WuqiRead.BATTERY, byteArrayOf(50)).battery())
    }

    @Test fun documentedWritesOnly() {
        assertArrayEquals(byteArrayOf(8, 0xEE.toByte(), 0, 0, 0, 2, 4, 11, 0, 1, 8), WuqiControls.wear(true))
        val eq = WuqiControls.equalizer(List(8) { 0 })
        assertEquals(32, eq.size)
        assertEquals(3, eq[5].toInt())
        assertEquals(0x81, eq[6].toInt() and 255)
        assertTrue(eq.slice(11..30).all { it == 120.toByte() })
        assertEquals(eq.dropLast(1).sumOf { it.toInt() and 255 } and 255, eq.last().toInt() and 255)
    }
}
