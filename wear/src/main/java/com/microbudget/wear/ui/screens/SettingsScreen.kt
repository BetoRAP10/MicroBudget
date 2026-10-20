package com.microbudget.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.microbudget.data.sync.ConnectionState
import com.microbudget.domain.format.DateFormats
import com.microbudget.wear.WearViewModel
import com.microbudget.wear.ui.components.StatRow
import com.microbudget.wear.ui.components.WearListScreen
import com.microbudget.wear.ui.theme.MbColors

@Composable
fun SettingsScreen(vm: WearViewModel, onRequestNotifications: () -> Unit) {
    val sync by vm.sync.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    WearListScreen {
        item { ListHeader { Text("Sincronización") } }
        item {
            StatRow(
                "Estado",
                when (sync.connection) {
                    ConnectionState.CONNECTED -> "Conectado"
                    ConnectionState.NO_PEER -> "Sin celular"
                    ConnectionState.API_UNAVAILABLE -> "No disponible"
                    ConnectionState.UNKNOWN -> "—"
                },
                if (sync.connection == ConnectionState.CONNECTED) MbColors.Green else MbColors.Orange,
            )
        }
        item {
            StatRow(
                "Pendientes", sync.pendingCount.toString(),
                if (sync.pendingCount == 0) MbColors.Green else MbColors.Orange,
            )
        }
        item { StatRow("Última", sync.lastSyncAt?.let { DateFormats.dateTime(it, vm.zone) } ?: "Nunca") }
        item { Text(sync.message, fontSize = 11.sp, color = MbColors.TextSecondary, textAlign = TextAlign.Center) }
        item {
            Chip(
                onClick = { vm.syncNow() }, modifier = Modifier.fillMaxWidth(),
                label = { Text(if (sync.inProgress) "Sincronizando…" else "Sincronizar ahora") },
                colors = ChipDefaults.primaryChipColors(backgroundColor = MbColors.Purple),
            )
        }
        item { ListHeader { Text("Datos de prueba") } }
        item {
            Chip(
                onClick = { vm.loadDemo() }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Cargar datos demo") },
                secondaryLabel = { Text("$2,000 mensual · 72 %") },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        item {
            Chip(
                onClick = { vm.resetAlerts() }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Rearmar alertas") },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        item {
            Chip(
                onClick = { vm.testNotification() }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Probar notificación") },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        if (!vm.hasNotificationPermission()) {
            item {
                Chip(
                    onClick = onRequestNotifications, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Permitir notificaciones") },
                    colors = ChipDefaults.primaryChipColors(backgroundColor = MbColors.Orange),
                )
            }
        }
        item {
            Chip(
                onClick = { if (confirmClear) { vm.clearAll(); confirmClear = false } else confirmClear = true },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (confirmClear) "Toca otra vez para borrar" else "Borrar datos del reloj", maxLines = 2) },
                colors = ChipDefaults.primaryChipColors(backgroundColor = MbColors.Red.copy(alpha = if (confirmClear) 1f else 0.5f)),
            )
        }
        item {
            Text(
                "Tus datos se guardan solo en este reloj y en tu celular. No hay respaldo en la nube.",
                fontSize = 10.sp, color = MbColors.TextSecondary, textAlign = TextAlign.Center,
            )
        }
    }
}
