// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.librebuds.protocol.boat

import java.util.Locale

enum class DriverFamily { HUAWEI, WUQI, BES, BLUETRUM, AIROHA, JIELI, GAIA, CEVA, STANDARD_BLE }
enum class Evidence { VERIFIED_MODEL_SERVICE, SERVICE_HINT, MODEL_HINT, UNKNOWN }

data class DriverCandidate(val family: DriverFamily, val evidence: Evidence)

/** Discovery evidence is not permission to send proprietary commands. */
object DriverRegistry {
    private val bluetrumHints = setOf("0000fdb3-0000-1000-8000-00805f9b34fb",
        "b6632277-0642-458b-a7a0-23fb1dc92c93")
    const val BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb"

    fun candidates(name: String?, services: Collection<String>): List<DriverCandidate> {
        val ids = services.map { it.lowercase(Locale.ROOT) }.toSet()
        val result = mutableListOf<DriverCandidate>()
        if (BoatDetection.isNirvanaIonAnc(name, services))
            result.add(DriverCandidate(DriverFamily.WUQI, Evidence.VERIFIED_MODEL_SERVICE))
        else if (BoatDetection.wuqiService.toString() in ids)
            result.add(DriverCandidate(DriverFamily.WUQI, Evidence.SERVICE_HINT))
        if (ids.any { it in bluetrumHints })
            result.add(DriverCandidate(DriverFamily.BLUETRUM, Evidence.SERVICE_HINT))
        val model = name?.trim()?.lowercase(Locale.ROOT)?.removePrefix("boat ")
        if (model in setOf("airdopes 800", "airdopes supreme", "nirvana ivy pro", "nirvana zenith pro"))
            result.add(DriverCandidate(DriverFamily.BES, Evidence.MODEL_HINT))
        if (BATTERY_SERVICE in ids)
            result.add(DriverCandidate(DriverFamily.STANDARD_BLE, Evidence.SERVICE_HINT))
        return result
    }

    /** Only the independently documented exact WuQi model/service pair may use vendor commands. */
    fun allowsWuqi(name: String?, services: Collection<String>): Boolean =
        candidates(name, services).any { it.family == DriverFamily.WUQI && it.evidence == Evidence.VERIFIED_MODEL_SERVICE }
}

/** Offline capture decoding only; these types never open connections or send bytes. */
data class CapturedFrame(val command: Int, val payload: List<Int>, val type: Int? = null)

object CaptureDecoders {
    private fun ByteArray.u(i: Int) = this[i].toInt() and 255

    fun bluetrum(bytes: ByteArray): CapturedFrame? {
        if (bytes.size !in 5..260 || bytes.u(0) > 15 || bytes.u(2) !in 1..3) return null
        val frag = bytes.u(3)
        if ((frag and 15) > (frag shr 4) || bytes.u(4) != bytes.size - 5) return null
        return CapturedFrame(bytes.u(1), (5 until bytes.size).map { bytes.u(it) }, bytes.u(2))
    }

    fun airoha(bytes: ByteArray): CapturedFrame? {
        if (bytes.size !in 6..4096 || bytes.u(0) != 5 || bytes.u(1) !in setOf(0x5A, 0x5B, 0x5D)) return null
        val length = bytes.u(2) or (bytes.u(3) shl 8)
        if (length != bytes.size - 4) return null
        return CapturedFrame(bytes.u(4) or (bytes.u(5) shl 8), (6 until bytes.size).map { bytes.u(it) }, bytes.u(1))
    }

    fun ceva(bytes: ByteArray): CapturedFrame? {
        if (bytes.size !in 5..259 || bytes.u(0) != 1 || bytes.u(1) != 0xE0 || bytes.u(2) != 0xFC) return null
        if (bytes.u(3) != bytes.size - 4) return null
        return CapturedFrame(bytes.u(4), (5 until bytes.size).map { bytes.u(it) })
    }

    /** Standard Bluetooth Battery Level characteristic (0x2A19). */
    fun standardBattery(bytes: ByteArray): Int? =
        if (bytes.size == 1) bytes.u(0).takeIf { it <= 100 } else null
}
