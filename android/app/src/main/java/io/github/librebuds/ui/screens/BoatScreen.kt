// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.librebuds.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.github.librebuds.boat.BoatClient
import io.github.librebuds.boat.BoatStatus
import io.github.librebuds.protocol.boat.WuqiNoiseMode
import io.github.librebuds.ui.PairRow
import io.github.librebuds.ui.components.StyledButton
import io.github.librebuds.ui.components.StyledScaffold
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun BoatScreen(pair: PairRow, backLabel: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val client = remember(pair.address) { BoatClient(context.applicationContext, pair.address) }
    val scope = rememberCoroutineScope()
    var status by remember(pair.address) { mutableStateOf<BoatStatus?>(null) }
    var busy by remember(pair.address) { mutableStateOf(false) }
    var error by remember(pair.address) { mutableStateOf<String?>(null) }
    suspend fun perform(action: suspend () -> BoatStatus) {
        if (busy) return
        busy = true
        error = null
        try {
            status = action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            status = null
            error = "Could not confirm earbud status. Reconnect and refresh."
        } finally {
            busy = false
        }
    }
    LaunchedEffect(pair.connected, client) {
        if (pair.connected) perform { client.refresh() } else status = null
    }
    val current = status.takeIf { pair.connected }
    val backdrop = rememberLayerBackdrop()
    StyledScaffold(title = pair.name, showBackButton = true, backLabel = backLabel, onNavigateBack = onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenContentPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Experimental boAt support", style = MaterialTheme.typography.titleMedium)
            Text(if (pair.connected) "Bluetooth connected" else "Connect your earbuds in Bluetooth settings.")
            Text(if (busy) "Communicating with earbuds…" else error ?: "Controls are enabled after a confirmed status read.")
            fun level(value: Int?) = value?.let { "$it%" } ?: "—"
            Text("Battery", style = MaterialTheme.typography.titleMedium)
            Text("Left: ${level(current?.battery?.left)}   Right: ${level(current?.battery?.right)}   Case: ${level(current?.battery?.case)}")
            StyledButton(onClick = { scope.launch { perform { client.refresh() } } }, backdrop = backdrop,
                enabled = pair.connected && !busy, modifier = Modifier.fillMaxWidth()) { Text("Refresh") }
            Text("Noise control", style = MaterialTheme.typography.titleMedium)
            WuqiNoiseMode.values().forEach { mode ->
                val label = when (mode) {
                    WuqiNoiseMode.OFF -> "Off"
                    WuqiNoiseMode.ANC -> "Noise cancellation"
                    WuqiNoiseMode.AMBIENT -> "Transparency"
                }
                StyledButton(onClick = { scope.launch { perform { client.setNoise(mode) } } }, backdrop = backdrop,
                    enabled = pair.connected && !busy && current?.noise != null, modifier = Modifier.fillMaxWidth()) {
                    Text(if (current?.noise == mode) "$label (active)" else label)
                }
            }
            Text("Wear detection", style = MaterialTheme.typography.titleMedium)
            StyledButton(onClick = { current?.wear?.let { enabled -> scope.launch { perform { client.setWear(!enabled) } } } },
                backdrop = backdrop, enabled = pair.connected && !busy && current?.wear != null,
                modifier = Modifier.fillMaxWidth()) {
                Text(when (current?.wear) { true -> "On — tap to disable"; false -> "Off — tap to enable"; null -> "Unknown" })
            }
        }
    }
}
