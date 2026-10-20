@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.microbudget.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.microbudget.domain.format.DateFormats
import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.money.Money
import com.microbudget.mobile.MobileViewModel
import com.microbudget.mobile.ui.theme.MbColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/** Formulario para registrar o editar un gasto (cantidad, categoría, fecha/hora, descripción). */
@Composable
fun ExpenseEditorScreen(vm: MobileViewModel, expenseId: String?, onClose: () -> Unit) {
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val existing = remember(expenseId) { expenseId?.let { id -> expenses.firstOrNull { it.id == id } } }
    val scope = rememberCoroutineScope()

    val initialDateTime = existing?.let { Instant.ofEpochMilli(it.dateTime).atZone(vm.zone).toLocalDateTime() } ?: LocalDateTime.now(vm.zone)
    var amount by remember { mutableStateOf(existing?.let { Money.toPlain(it.amount) } ?: "") }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var date by remember { mutableStateOf(initialDateTime.toLocalDate()) }
    var time by remember { mutableStateOf(initialDateTime.toLocalTime().withSecond(0).withNano(0)) }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (existing == null) "Nuevo gasto" else "Editar gasto") },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
            actions = {
                if (existing != null) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MbColors.Red) }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MbColors.Background),
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(12); error = null },
                label = { Text("Cantidad") },
                prefix = { Text("$") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error != null,
                supportingText = { error?.let { Text(it, color = MbColors.Red) } },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, color = MbColors.Text),
            )
            Text("Categoría", color = MbColors.TextSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEach { c ->
                    FilterChip(
                        selected = categoryId == c.id,
                        onClick = { categoryId = c.id; error = null },
                        label = { Text("${c.icon} ${c.name}") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MbColors.Purple, selectedLabelColor = MbColors.Text),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(DateFormats.shortDay(date))
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("%02d:%02d".format(time.hour, time.minute))
                }
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(80) },
                label = { Text("Descripción (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            existing?.let {
                Text(
                    "Registrado desde el ${if (it.createdFrom == DeviceSource.WATCH) "reloj" else "celular"} · " +
                        if (it.syncStatus.name == "SYNCED") "sincronizado" else "pendiente de sincronizar",
                    color = MbColors.TextSecondary, fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    val millis = LocalDateTime.of(date, time).atZone(vm.zone).toInstant().toEpochMilli()
                    scope.launch {
                        val err = vm.saveExpense(existing?.id, amount, categoryId, millis, description)
                        if (err == null) onClose() else error = err
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MbColors.Purple),
            ) { Text(if (existing == null) "Guardar gasto" else "Guardar cambios", fontSize = 16.sp) }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDate = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state) }
    }
    if (showTime) {
        val state = rememberTimePickerState(time.hour, time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = { TextButton(onClick = { time = LocalTime.of(state.hour, state.minute); showTime = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancelar") } },
            text = { TimePicker(state) },
        )
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar gasto?") },
            text = { Text("Se eliminará ${Money.format(existing.amount)} y los totales se recalcularán. El borrado también se enviará al reloj.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteExpense(existing.id); confirmDelete = false; onClose() }) { Text("Eliminar", color = MbColors.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}
