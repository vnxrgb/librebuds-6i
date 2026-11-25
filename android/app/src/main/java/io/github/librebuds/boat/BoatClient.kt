// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.librebuds.boat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import io.github.librebuds.companion.BondedDevice
import io.github.librebuds.companion.readBondedDevices
import io.github.librebuds.protocol.boat.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

fun detectBoat(bonded: List<BondedDevice>): List<BondedDevice> =
    bonded.filter { BoatDetection.isNirvanaIonAnc(it.name, it.uuids) || HiDefIdentity.matches(it.name) }

data class BoatStatus(
    val battery: WuqiBattery? = null,
    val noise: WuqiNoiseMode? = null,
    val wear: Boolean? = null
)

/** Dedicated foreground transactions: no Huawei handshake, no background writes. */
class BoatClient(private val context: Context, private val address: String) {
    private val lock = Mutex()

    suspend fun refresh(): BoatStatus = transact { readStatus(it) }

    suspend fun setNoise(mode: WuqiNoiseMode): BoatStatus = transact { channel ->
        check(channel.query(WuqiRead.NOISE_MODE).noiseMode() != null)
        channel.write(WuqiControls.noise(mode))
        delay(350)
        readStatus(channel).also { check(it.noise == mode) { "Noise mode not confirmed" } }
    }

    suspend fun setWear(enabled: Boolean): BoatStatus = transact { channel ->
        check(channel.query(WuqiRead.WEAR_DETECTION).wearDetection() != null)
        channel.write(WuqiControls.wear(enabled))
        delay(350)
        readStatus(channel).also { check(it.wear == enabled) { "Wear setting not confirmed" } }
    }

    // No documented curve readback. An EQ write is deliberately not exposed in the UI yet.
    private fun readStatus(channel: Channel): BoatStatus {
        val battery = channel.query(WuqiRead.BATTERY).battery()
        val noise = channel.query(WuqiRead.NOISE_MODE).noiseMode()
        val wear = channel.query(WuqiRead.WEAR_DETECTION).wearDetection()
        return BoatStatus(battery, noise, wear)
    }

    private class Channel(private val socket: BluetoothSocket) {
        private val parser = WuqiReplyParser()
        fun write(bytes: ByteArray) {
            socket.outputStream.write(bytes)
            socket.outputStream.flush()
        }
        fun query(command: WuqiRead): WuqiReply {
            write(command.request())
            val bytes = ByteArray(512)
            while (true) {
                val n = socket.inputStream.read(bytes)
                if (n <= 0) throw IOException("Earbuds closed connection")
                parser.feed(bytes.copyOf(n)).firstOrNull { it.command == command }?.let { return it }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun <T> transact(operation: suspend (Channel) -> T): T = lock.withLock {
        check(readBondedDevices(context).orEmpty().any { it.address.equals(address, true) && BoatDetection.isNirvanaIonAnc(it.name, it.uuids) }) {
            "Unsupported or unpaired boAt device"
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: error("Bluetooth unavailable")
        val socket = adapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(BoatDetection.wuqiService)
        coroutineScope {
            // A timeout closes the socket, including a blocked connect/read.
            val watchdog = launch(Dispatchers.Default) {
                delay(12000)
                runCatching { socket.close() }
            }
            try {
                withContext(Dispatchers.IO) {
                    socket.connect()
                    operation(Channel(socket))
                }
            } finally {
                watchdog.cancel()
                runCatching { socket.close() }
            }
        }
    }
}
