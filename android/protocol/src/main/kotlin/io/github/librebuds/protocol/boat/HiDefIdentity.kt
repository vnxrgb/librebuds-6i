// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.librebuds.protocol.boat

/** Identity only; Bluetooth names never authorize vendor writes. */
object HiDefIdentity {
    fun matches(name: String?): Boolean {
        val normalized = name?.trim()?.replace(Regex("\\s+"), " ") ?: return false
        return normalized.equals("Airdopes 800 HiDef", true) ||
            normalized.equals("boAt Airdopes 800 HiDef", true) ||
            normalized.equals("Airdopes 800 Hi-Def", true) ||
            normalized.equals("boAt Airdopes 800 Hi-Def", true)
    }
}

/** Offline-only BES framing research. No transport or command transmission is implemented.
 * Length is the number of bytes after the four-byte header. Cap untrusted frames.
 */
data class BesFrame(val command: Int, val arguments: List<Int>)

object BesFrameDecoder {
    fun decode(frame: ByteArray): BesFrame? {
        if (frame.size !in 5..260) return null
        fun u(i: Int) = frame[i].toInt() and 255
        if (u(0) != 8 || u(1) != 0x63) return null
        val length = u(2) or (u(3) shl 8)
        if (length != frame.size - 4) return null
        return BesFrame(u(4), (5 until frame.size).map(::u))
    }
}
