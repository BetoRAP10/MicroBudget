@file:OptIn(ExperimentalMaterial3Api::class)

package com.microbudget.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.microbudget.data.sync.ConnectionState
import com.microbudget.domain.format.DateFormats
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.money.Money
import com.microbudget.mobile.MobileViewModel
import com.microbudget.mobile.ui.components.MbCard
import com.microbudget.mobile.ui.components.SectionTitle
import com.microbudget.mobile.ui.theme.MbColors

@Composable
fun SettingsScreen(vm: MobileViewModel, onCategories: () -> Unit, onRequestNotifications: () -> Unit) {
    val budget by vm.budget.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()

    var amount by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var at80 by remember { mutableStateOf(true) }
    var at100 by remember { mutableStateOf(true) }
    var customOn by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    // Rellena el formulario con los valores guardados (también cuando llegan del reloj).
    LaunchedEffect(budget) {
        budget?.let { b ->
            amount = Money.toPlain(b.amount)
            period = b.period
            at80 = b.alertAt80
            at100 = b.alertAt100
            customOn = b.customAlertAmount != null
            custom = b.customAlertAmount?.let { Money.toPlain(it) } ?: ""
        }
    }
    val switchColors = SwitchDefaults.colors(checkedTrackColor = MbColors.Purple)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }

        item {
            MbCard {
                SectionTitle("Presupuesto")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    BudgetPeriod.entries.forEachIndexed { i, p ->
                        SegmentedButton(
                            selected = period == p,
                            onClick = { period = p },
                            shape = SegmentedButtonDefaults.itemShape(i, BudgetPeriod.entries.size),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = MbColors.Purple, activeContentColor = MbColors.Text),
                        ) { Text(if (p == BudgetPeriod.WEEKLY) "Semanal" else "Mensual") }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Monto del presupuesto") }, prefix = { Text("$") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                summary?.let { Text("Periodo actual: ${it.periodLabel}", color = MbColors.TextSecondary, fontSize = 12.sp) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.setBudget(amount, period) },
                    colors = ButtonDefaults.buttonColors(containerColor = MbColors.Purple), modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar presupuesto") }
            }
        }

        item {
            MbCard {
                SectionTitle("Alertas")
                ToggleRow("Avisar al 80 %", at80, switchColors) { at80 = it }
                ToggleRow("Avisar al 100 %", at100, switchColors) { at100 = it }
                ToggleRow("Límite personalizado", customOn, switchColors) { customOn = it }
                if (customOn) {
                    OutlinedTextField(
                        value = custom, onValueChange = { custom = it },
                        label = { Text("Avisar cuando el gasto llegue a") }, prefix = { Text("$") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    "Cada alerta se muestra una sola vez por periodo y se restablece al iniciar el siguiente.",
                    color = MbColors.TextSecondary, fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.setAlerts(at80, at100, customOn, custom) },
                    colors = ButtonDefaults.buttonColors(containerColor = MbColors.Purple), modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar alertas") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { vm.testNotification() }, modifier = Modifier.weight(1f)) { Text("Probar aviso") }
                    OutlinedButton(onClick = { vm.resetAlerts() }, modifier = Modifier.weight(1f)) { Text("Rearmar alertas") }
                }
                if (!vm.hasNotificationPermission()) {
                    Text("Las notificaciones están desactivadas; verás los avisos dentro de la app.", color = MbColors.Orange, fontSize = 12.sp)
                    TextButton(onClick = onRequestNotifications) { Text("Permitir notificaciones") }
                }
            }
        }

        item {
            MbCard(onClick = onCategories) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Administrar categorías", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }

        item {
            MbCard {
                SectionTitle("Sincronización con el reloj")
                val (label, color) = when (sync.connection) {
                    ConnectionState.CONNECTED -> "Conectado${sync.peerName?.let { " · $it" } ?: ""}" to MbColors.Green
                    ConnectionState.NO_PEER -> "Reloj no conectado" to MbColors.Orange
                    ConnectionState.API_UNAVAILABLE -> "Wear OS no disponible en este teléfono" to MbColors.Orange
                    ConnectionState.UNKNOWN -> "Sin comprobar" to MbColors.TextSecondary
                }
                Text(label, color = color, fontWeight = FontWeight.SemiBold)
                Text(
                    if (sync.pendingCount == 0) "Todos los cambios están sincronizados."
                    else "${sync.pendingCount} cambios aún NO se han sincronizado con el reloj.",
                    color = if (sync.pendingCount == 0) MbColors.Green else MbColors.Orange,
                )
                Text("Última sincronización: ${sync.lastSyncAt?.let { DateFormats.dateTime(it, vm.zone) } ?: "nunca"}", color = MbColors.TextSecondary, fontSize = 13.sp)
                Text(sync.message, color = MbColors.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.syncNow() }, colors = ButtonDefaults.buttonColors(containerColor = MbColors.Purple), modifier = Modifier.weight(1f)) {
                        Text(if (sync.inProgress) "Sincronizando…" else "Sincronizar")
                    }
                    OutlinedButton(onClick = { vm.syncNow(full = true) }, modifier = Modifier.weight(1f)) { Text("Sync completa") }
                }
                Text(
                    "Conflictos: si un gasto se edita en ambos dispositivos, se conserva la modificación más reciente.",
                    color = MbColors.TextSecondary, fontSize = 12.sp,
                )
            }
        }

        item {
            MbCard {
                SectionTitle("Datos de prueba")
                Text("Carga un presupuesto de $2,000 con gastos ficticios (72.5 % usado) para demostrar alertas y gráficas.", color = MbColors.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.loadDemo() }, colors = ButtonDefaults.buttonColors(containerColor = MbColors.PurpleDark), modifier = Modifier.weight(1f)) { Text("Cargar demo") }
                    OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.weight(1f)) { Text("Borrar todo", color = MbColors.Red) }
                }
            }
        }

        item {
            MbCard {
                SectionTitle("Privacidad")
                Text(
                    "• MicroBudget no pide ni guarda datos bancarios: registras tus gastos a mano.\n" +
                        "• Solo se guarda cantidad, categoría, fecha y una descripción opcional.\n" +
                        "• Los datos viven en este teléfono y en tu reloj. No hay respaldo en la nube.",
                    color = MbColors.TextSecondary, fontSize = 13.sp,
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("¿Borrar todos los datos?") },
            text = { Text("Se eliminarán los gastos, categorías personalizadas y la configuración de ESTE teléfono. No afecta al reloj.") },
            confirmButton = { TextButton(onClick = { vm.clearAll(); confirmClear = false }) { Text("Borrar", color = MbColors.Red) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, colors: androidx.compose.material3.SwitchColors, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = colors)
    }
}
