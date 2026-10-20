package com.microbudget.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import com.microbudget.domain.budget.AffordVerdict
import com.microbudget.domain.budget.PeriodCalculator
import com.microbudget.domain.money.Money
import com.microbudget.wear.WearViewModel
import com.microbudget.wear.ui.components.StatRow
import com.microbudget.wear.ui.components.WearListScreen
import com.microbudget.wear.ui.theme.MbColors

/** Resumen del presupuesto + configuración rápida (monto, periodo y alertas). */
@Composable
fun BudgetScreen(vm: WearViewModel, onEditAmount: () -> Unit, onEditCustomAlert: () -> Unit) {
    val s by vm.summary.collectAsStateWithLifecycle()
    val budget by vm.budget.collectAsStateWithLifecycle()

    WearListScreen {
        item { ListHeader { Text("Presupuesto") } }
        val summary = s
        if (summary != null) {
            item { Text(summary.periodLabel, fontSize = 12.sp, color = MbColors.PurpleLight, textAlign = TextAlign.Center) }
            item { StatRow("Inicial", Money.formatShort(summary.budgetAmount)) }
            item { StatRow("Gastado", Money.formatShort(summary.spent)) }
            item {
                if (summary.isExceeded) StatRow("Exceso", Money.formatShort(summary.overspent), MbColors.Red)
                else StatRow("Disponible", Money.formatShort(summary.available), MbColors.forStatus(summary.status))
            }
            item { StatRow("Consumido", "${summary.percentUsed} %", MbColors.forStatus(summary.status)) }
            item { StatRow("Promedio/día", Money.formatShort(summary.dailyAverage)) }
            item { StatRow("Puedes/día", Money.formatShort(summary.dailyAllowance)) }
            item { StatRow("Días restantes", summary.daysRemaining.toString()) }
        }
        item {
            Chip(
                onClick = onEditAmount, modifier = Modifier.fillMaxWidth(),
                label = { Text("Cambiar monto") },
                secondaryLabel = { Text(budget?.let { Money.formatShort(it.amount) } ?: "") },
                colors = ChipDefaults.primaryChipColors(backgroundColor = MbColors.Purple),
            )
        }
        item {
            Chip(
                onClick = { vm.togglePeriod() }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Periodo: ${budget?.let { PeriodCalculator.displayName(it.period) } ?: ""}") },
                secondaryLabel = { Text("Toca para cambiar") },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        item { ListHeader { Text("Alertas") } }
        item {
            ToggleChip(
                checked = budget?.alertAt80 == true,
                onCheckedChange = { vm.setAlerts(at80 = it) },
                label = { Text("Aviso al 80 %") },
                toggleControl = { Switch(checked = budget?.alertAt80 == true) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            ToggleChip(
                checked = budget?.alertAt100 == true,
                onCheckedChange = { vm.setAlerts(at100 = it) },
                label = { Text("Aviso al 100 %") },
                toggleControl = { Switch(checked = budget?.alertAt100 == true) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Chip(
                onClick = onEditCustomAlert, modifier = Modifier.fillMaxWidth(),
                label = { Text("Límite personalizado") },
                secondaryLabel = { Text(budget?.customAlertAmount?.let { Money.formatShort(it) } ?: "Desactivado") },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        if (budget?.customAlertAmount != null) {
            item {
                Chip(
                    onClick = { vm.setAlerts(clearCustom = true) }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quitar límite") },
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
        }
    }
}

/** Resultado de «¿Me alcanza?». No guarda nada; ofrece registrar el gasto si el usuario quiere. */
@Composable
fun AffordResultScreen(vm: WearViewModel, cents: Long, onRegister: () -> Unit, onBack: () -> Unit) {
    val result = vm.affordability(cents)
    WearListScreen {
        if (result == null) {
            item { Text("Calculando…") }
            return@WearListScreen
        }
        val color = MbColors.forVerdict(result.verdict)
        item {
            Box(Modifier.size(48.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                Text(
                    when (result.verdict) { AffordVerdict.YES -> "✓"; AffordVerdict.TIGHT -> "!"; AffordVerdict.NO -> "✕" },
                    fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MbColors.Background,
                )
            }
        }
        item { Text(Money.format(result.amount), fontSize = 14.sp, color = MbColors.TextSecondary) }
        item { Text(result.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center) }
        item { Text(result.message, fontSize = 12.sp, color = MbColors.Text, textAlign = TextAlign.Center) }
        item {
            Chip(
                onClick = onRegister, modifier = Modifier.fillMaxWidth(),
                label = { Text("Registrar este gasto") },
                colors = ChipDefaults.primaryChipColors(backgroundColor = MbColors.Purple),
            )
        }
        item {
            Chip(onClick = onBack, modifier = Modifier.fillMaxWidth(), label = { Text("Volver") }, colors = ChipDefaults.secondaryChipColors())
        }
    }
}
