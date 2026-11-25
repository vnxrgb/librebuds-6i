// LibreBuds contributors - SPDX-License-Identifier: GPL-3.0-or-later
package io.github.librebuds.protocol.boat

import java.util.UUID

/** Conservative routing: only the documented hardware-tested model and service qualify. */
object BoatDetection {
    val wuqiService: UUID = UUID.fromString("00007034-0000-1000-8000-00805f9b34fb")

    fun isNirvanaIonAnc(name: String?, services: Collection<String>): Boolean {
        val normalized = name?.trim()?.replace(Regex("\\s+"), " ") ?: return false
        if (!normalized.equals("Nirvana Ion ANC", true) &&
            !normalized.equals("boAt Nirvana Ion ANC", true)) return false
        return services.any { runCatching { UUID.fromString(it) }.getOrNull() == wuqiService }
    }
}

/** Read-only allowlist. No generic command builder or write/reset API is exposed. */
enum class WuqiRead(val code: Int) {
    TWS_LINK(0x0104), BATTERY(0x0105), NOISE_MODE(0x010C), WEAR_DETECTION(0x0109);

    fun request(): ByteArray {
        val frame = byteArrayOf(8, 0xEE.toByte(), 0, 0, 0, (code shr 8).toByte(),
            code.toByte(), 10, 0, 0)
        frame[9] = frame.take(9).sumOf { it.toInt() and 255 }.toByte()
        return frame
    }
}

data class WuqiReply(val command: WuqiRead, val payload: ByteArray)

/** Bounded stream parser; drops unknown commands, invalid checksum and malformed headers. */
class WuqiReplyParser {
    private val pending = ArrayList<Byte>(255)
    private val header = byteArrayOf(9, 0xFF.toByte(), 0, 0, 1)

    fun feed(bytes: ByteArray): List<WuqiReply> {
        val result = mutableListOf<WuqiReply>()
        for (byte in bytes) {
            pending.add(byte)
            while (pending.isNotEmpty()) {
                val prefix = minOf(pending.size, header.size)
                if ((0 until prefix).any { pending[it] != header[it] }) {
                    pending.removeAt(0)
                    continue
                }
                if (pending.size < 9) break
                val length = pending[7].toInt() and 255
                if (length < 10 || pending[8] != 0.toByte()) {
                    pending.removeAt(0)
                    continue
                }
                if (pending.size < length) break
                val frame = pending.take(length)
                if ((frame.take(length - 1).sumOf { it.toInt() and 255 } and 255) !=
                    (frame.last().toInt() and 255)) {
                    pending.removeAt(0)
                    continue
                }
                repeat(length) { pending.removeAt(0) }
                val code = ((frame[5].toInt() and 255) shl 8) or (frame[6].toInt() and 255)
                val command = WuqiRead.values().firstOrNull { it.code == code } ?: continue
                result.add(WuqiReply(command, frame.subList(9, length - 1).toByteArray()))
            }
        }
        return result
    }
}

enum class WuqiNoiseMode { OFF, ANC, AMBIENT }

fun WuqiReply.noiseMode(): WuqiNoiseMode? =
    if (command == WuqiRead.NOISE_MODE) payload.lastOrNull()?.let {
        when (it.toInt() and 255) {
            0 -> WuqiNoiseMode.OFF
            1 -> WuqiNoiseMode.ANC
            2 -> WuqiNoiseMode.AMBIENT
            else -> null
        }
    } else null
