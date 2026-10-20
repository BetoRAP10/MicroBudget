package com.microbudget.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.microbudget.domain.budget.AffordabilityResult
import com.microbudget.domain.budget.AlertType
import com.microbudget.domain.budget.BudgetStatus
import com.microbudget.domain.model.BudgetPeriod
import com.microbudget.domain.money.Money
import com.microbudget.domain.stats.StatsCalculator
import com.microbudget.mobile.MobileViewModel
import com.microbudget.mobile.ui.components.AppLogo
import com.microbudget.mobile.ui.components.Banner
import com.microbudget.mobile.ui.components.DonutChart
import com.microbudget.mobile.ui.components.DonutSlice
import com.microbudget.mobile.ui.components.ExpenseRow
import com.microbudget.mobile.ui.components.MbCard
import com.microbudget.mobile.ui.components.SectionTitle
import com.microbudget.mobile.ui.components.SummaryCard
import com.microbudget.mobile.ui.components.SyncPill
import com.microbudget.mobile.ui.theme.MbColors

@Composable
fun DashboardScreen(vm: MobileViewModel, onOpenExpense: (String) -> Unit, onSeeAll: () -> Unit) {
    val summary by vm.summary.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val alert by vm.latestAlert.collectAsStateWithLifecycle()
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val periodExpenses by vm.periodExpenses.collectAsStateWithLifecycle()
    val budget by vm.budget.collectAsStateWithLifecycle()
    val period = budget?.period ?: BudgetPeriod.MONTHLY

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo()
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("MicroBudget", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Control de gastos instantáneo", color = MbColors.TextSecondary, fontSize = 12.sp)
                }
                SyncPill(sync) { vm.syncNow() }
            }
        }
        alert?.let { a ->
            item {
                Banner(
                    "⚠ ${a.title}: ${a.message}",
                    if (a.type == AlertType.PERCENT_80) MbColors.Orange else MbColors.Red,
                ) { TextButton(onClick = { vm.dismissAlert() }) { Text("OK") } }
            }
        }
        summary?.let { s ->
            item { SummaryCard(s) }
            if (alert == null && s.status != BudgetStatus.OK) {
                item {
                    Banner(
                        if (s.isExceeded) "Superaste tu presupuesto por ${Money.format(s.overspent)}."
                        else "Ya usaste el ${s.percentUsed} % de tu presupuesto.",
                        MbColors.forStatus(s.status),
                    )
                }
            }
        }
        item { AffordCard(vm) }
        item {
            MbCard {
                SectionTitle("Gasto por categoría")
                val totals = StatsCalculator.byCategory(periodExpenses)
                if (totals.isEmpty()) {
                    Text("Sin gastos en este periodo.", color = MbColors.TextSecondary)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(
                            totals.map { t -> vm.category(t.categoryId).let { DonutSlice(it.name, t.total.toFloat(), Color(it.colorArgb)) } },
                            summary?.spent ?: Money.ZERO, sizeDp = 130,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            totals.take(4).forEach { t ->
                                val c = vm.category(t.categoryId)
                                Text("${c.icon} ${c.name}", fontSize = 13.sp)
                                Text("${Money.format(t.total)} · ${(t.share * 100).toInt()} %", color = MbColors.TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("Recientes", trailing = { TextButton(onClick = onSeeAll) { Text("Ver todo") } }) }
        if (expenses.isEmpty()) {
            item { Text("Aún no hay gastos. Usa «Nuevo gasto» o carga datos de prueba en Ajustes.", color = MbColors.TextSecondary) }
        }
        items(expenses.take(5), key = { it.id }) { e ->
            ExpenseRow(e, vm.category(e.categoryId), vm.zone, period) { onOpenExpense(e.id) }
        }
        item {
            Text(
                "Tus datos se guardan solo en este teléfono y en tu reloj. MicroBudget no tiene respaldo en la nube ni se conecta a tu banco.",
                color = MbColors.TextSecondary, fontSize = 12.sp,
            )
        }
    }
}

/** Función «¿Me alcanza?»: simula un gasto sin guardarlo. */
@Composable
private fun AffordCard(vm: MobileViewModel) {
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<AffordabilityResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    MbCard {
        SectionTitle("¿Me alcanza?")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; result = null; error = null },
                label = { Text("Cantidad") },
                prefix = { Text("$") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error != null,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { vm.affordability(text).onSuccess { result = it }.onFailure { error = it.message } },
                colors = ButtonDefaults.buttonColors(containerColor = MbColors.Purple),
            ) { Text("Calcular") }
        }
        error?.let { Text(it, color = MbColors.Red, fontSize = 13.sp) }
        result?.let { r ->
            Spacer(Modifier.height(8.dp))
            Text(r.title, color = MbColors.forVerdict(r.verdict), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(r.message, color = MbColors.Text, modifier = Modifier.fillMaxWidth())
        }
    }
}
