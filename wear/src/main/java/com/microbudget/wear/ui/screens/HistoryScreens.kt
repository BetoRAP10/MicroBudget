package com.microbudget.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.microbudget.domain.budget.PeriodCalculator
import com.microbudget.domain.format.DateFormats
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.model.DeviceSource
import com.microbudget.domain.model.SyncStatus
import com.microbudget.domain.money.Money
import com.microbudget.wear.WearViewModel
import com.microbudget.wear.ui.components.CategoryBadge
import com.microbudget.wear.ui.components.StatRow
import com.microbudget.wear.ui.components.WearListScreen
import com.microbudget.wear.ui.theme.MbColors

/** Historial reciente: máximo 10 movimientos para mantener la interfaz sencilla. */
@Composable
fun HistoryScreen(vm: WearViewModel, onOpen: (String) -> Unit) {
    val recent by vm.recent.collectAsStateWithLifecycle()
    val budget by vm.budget.collectAsStateWithLifecycle()
    val period = budget?.period ?: BudgetPeriod.MONTHLY

    WearListScreen {
        item { ListHeader { Text("Recientes") } }
        if (recent.isEmpty()) {
            item { Text("Aún no hay gastos", color = MbColors.TextSecondary, textAlign = TextAlign.Center) }
        }
        recent.forEach { e ->
            item(key = e.id) {
                val c = vm.category(e.categoryId)
                Chip(
                    onClick = { onOpen(e.id) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = { CategoryBadge(c, 26) },
                    label = {
                        Text(
                            "${Money.format(e.amount)} · ${c.name}",
                            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    },
                    secondaryLabel = {
                        Text(
                            (if (e.syncStatus == SyncStatus.PENDING) "⟳ " else "") +
                                "${DateFormats.dateTime(e.dateTime, vm.zone)} · ${PeriodCalculator.shortLabelFor(period, e.dateTime, vm.zone)}",
                            maxLines = 1, fontSize = 11.sp,
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
        }
        item {
            Text(
                "⟳ = pendiente de sincronizar",
                fontSize = 10.sp, color = MbColors.TextSecondary, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ExpenseDetailScreen(vm: WearViewModel, id: String, onDeleted: () -> Unit) {
    val recent by vm.recent.collectAsStateWithLifecycle()
    val budget by vm.budget.collectAsStateWithLifecycle()
    val e = recent.firstOrNull { it.id == id }
    var confirmDelete by remember { mutableStateOf(false) }

    WearListScreen {
        if (e == null) {
            item { Text("Gasto no disponible", color = MbColors.TextSecondary) }
            return@WearListScreen
        }
        val c = vm.category(e.categoryId)
        item { CategoryBadge(c, 34) }
        item { Text(Money.format(e.amount), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MbColors.Text) }
        item { Text(c.name, color = MbColors.TextSecondary) }
        item { StatRow("Fecha", DateFormats.dateTime(e.dateTime, vm.zone)) }
        item { StatRow("Periodo", PeriodCalculator.shortLabelFor(budget?.period ?: BudgetPeriod.MONTHLY, e.dateTime, vm.zone)) }
        item { StatRow("Origen", if (e.createdFrom == DeviceSource.WATCH) "Reloj" else "Celular") }
        item {
            StatRow(
                "Sync",
                if (e.syncStatus == SyncStatus.SYNCED) "Sincronizado" else "Pendiente",
                if (e.syncStatus == SyncStatus.SYNCED) MbColors.Green else MbColors.Orange,
            )
        }
        e.description?.let { d -> item { Text(d, fontSize = 12.sp, color = MbColors.TextSecondary, textAlign = TextAlign.Center) } }
        item {
            Chip(
                onClick = { if (confirmDelete) vm.deleteExpense(e.id, onDeleted) else confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (confirmDelete) "Toca de nuevo para borrar" else "Eliminar", maxLines = 2) },
                icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                colors = ChipDefaults.primaryChipColors(backgroundColor = MbColors.Red.copy(alpha = if (confirmDelete) 1f else 0.55f)),
            )
        }
    }
}
